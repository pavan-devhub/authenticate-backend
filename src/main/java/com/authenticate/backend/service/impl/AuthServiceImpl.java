package com.authenticate.backend.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authenticate.backend.dto.request.LoginRequest;
import com.authenticate.backend.dto.request.RegisterRequest;
import com.authenticate.backend.dto.request.ResendOtpRequest;
import com.authenticate.backend.dto.request.VerifyOtpRequest;
import com.authenticate.backend.dto.response.LoginAttemptResponse;
import com.authenticate.backend.dto.response.LoginResponse;
import com.authenticate.backend.dto.response.OtpChallengeResponse;
import com.authenticate.backend.dto.response.UserResponse;
import com.authenticate.backend.entity.LoginAttempt;
import com.authenticate.backend.entity.LoginOutcome;
import com.authenticate.backend.entity.OtpChallenge;
import com.authenticate.backend.entity.OtpPurpose;
import com.authenticate.backend.entity.User;
import com.authenticate.backend.exception.EmailAlreadyExistsException;
import com.authenticate.backend.exception.InvalidCredentialsException;
import com.authenticate.backend.exception.InvalidOtpException;
import com.authenticate.backend.exception.OtpExpiredException;
import com.authenticate.backend.mapper.LoginAttemptMapper;
import com.authenticate.backend.mapper.OtpChallengeMapper;
import com.authenticate.backend.mapper.UserMapper;
import com.authenticate.backend.repository.LoginAttemptRepository;
import com.authenticate.backend.repository.UserRepository;
import com.authenticate.backend.service.AuthService;
import com.authenticate.backend.service.OtpCheckResult;
import com.authenticate.backend.service.OtpService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Limit LOGIN_HISTORY_SIZE = Limit.of(50);

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserRepository userRepository,
            LoginAttemptRepository loginAttemptRepository,
            OtpService otpService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OtpChallengeResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        OtpChallenge challenge = otpService.startRegistration(
                request.name().trim(), email, passwordEncoder.encode(request.password()));
        return OtpChallengeMapper.toResponse(challenge);
    }

    @Override
    // A wrong or expired code changes the challenge (an attempt used up, or the
    // challenge removed) before the exception is thrown, so those exceptions
    // must commit the transaction instead of rolling the change back.
    @Transactional(noRollbackFor = {InvalidOtpException.class, OtpExpiredException.class})
    public UserResponse verifyRegistration(VerifyOtpRequest request) {
        OtpCheckResult check = otpService.check(request.challengeId(), OtpPurpose.REGISTRATION, request.code());
        if (!check.correct()) {
            throw check.failure();
        }

        OtpChallenge challenge = check.challenge();
        User user = new User(challenge.getName(), challenge.getEmail(), challenge.getPasswordHash());
        try {
            return UserMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Someone else confirmed an account with this email after the code was sent.
            throw new EmailAlreadyExistsException();
        }
    }

    @Override
    @Transactional(noRollbackFor = OtpExpiredException.class)
    public OtpChallengeResponse resendCode(ResendOtpRequest request) {
        return OtpChallengeMapper.toResponse(otpService.resend(request.challengeId()));
    }

    @Override
    // A wrong password is saved first and then reported by throwing, so that
    // exception must commit the transaction instead of rolling the record back.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse login(LoginRequest request) {
        // No account means no history to record the attempt in.
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptRepository.save(new LoginAttempt(user, LoginOutcome.WRONG_PASSWORD));
            throw new InvalidCredentialsException();
        }

        loginAttemptRepository.save(new LoginAttempt(user, LoginOutcome.SUCCESS));
        List<LoginAttemptResponse> history = loginAttemptRepository
                .findByUserOrderByAttemptedAtDesc(user, LOGIN_HISTORY_SIZE)
                .stream()
                .map(LoginAttemptMapper::toResponse)
                .toList();
        return new LoginResponse(UserMapper.toResponse(user), history);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
