package com.authenticate.backend.service.impl;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.authenticate.backend.config.OtpProperties;
import com.authenticate.backend.entity.OtpChallenge;
import com.authenticate.backend.entity.OtpPurpose;
import com.authenticate.backend.exception.OtpExpiredException;
import com.authenticate.backend.exception.OtpResendTooSoonException;
import com.authenticate.backend.repository.OtpChallengeRepository;
import com.authenticate.backend.service.EmailService;
import com.authenticate.backend.service.OtpCheckResult;
import com.authenticate.backend.service.OtpService;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final OtpChallengeRepository otpChallengeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties properties;
    private final Clock clock;

    public OtpServiceImpl(
            OtpChallengeRepository otpChallengeRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            OtpProperties properties,
            Clock clock) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public OtpChallenge startRegistration(String name, String email, String passwordHash) {
        otpChallengeRepository.deleteByPurposeAndEmail(OtpPurpose.REGISTRATION, email);
        return sendNewCode(OtpChallenge.forRegistration(newToken(), name, email, passwordHash));
    }

    @Override
    public OtpChallenge resend(String token) {
        OtpChallenge challenge = findActive(token);
        Instant now = clock.instant();
        if (now.isBefore(challenge.getResendAvailableAt())) {
            long secondsLeft = (Duration.between(now, challenge.getResendAvailableAt()).toMillis() + 999) / 1000;
            throw new OtpResendTooSoonException(secondsLeft);
        }
        return sendNewCode(challenge);
    }

    @Override
    public OtpCheckResult check(String token, OtpPurpose purpose, String code) {
        OtpChallenge challenge = findActive(token);
        if (challenge.getPurpose() != purpose) {
            throw OtpExpiredException.notFound();
        }

        if (passwordEncoder.matches(code, challenge.getCodeHash())) {
            otpChallengeRepository.delete(challenge);
            return new OtpCheckResult(challenge, true, 0);
        }

        challenge.recordFailedAttempt();
        int attemptsLeft = Math.max(0, properties.maxAttempts() - challenge.getFailedAttempts());
        if (attemptsLeft == 0) {
            otpChallengeRepository.delete(challenge);
        }
        return new OtpCheckResult(challenge, false, attemptsLeft);
    }

    // Unfinished registrations hold a password hash, so don't keep them around.
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    public void deleteExpiredChallenges() {
        int deleted = otpChallengeRepository.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Deleted {} expired verification codes", deleted);
        }
    }

    private OtpChallenge findActive(String token) {
        OtpChallenge challenge = otpChallengeRepository.findByToken(token)
                .orElseThrow(OtpExpiredException::notFound);
        if (challenge.isExpiredAt(clock.instant())) {
            otpChallengeRepository.delete(challenge);
            throw OtpExpiredException.expired();
        }
        return challenge;
    }

    private OtpChallenge sendNewCode(OtpChallenge challenge) {
        String code = newCode();
        Instant now = clock.instant();
        challenge.replaceCode(
                passwordEncoder.encode(code),
                now.plus(properties.validity()),
                now.plus(properties.resendCooldown()));
        OtpChallenge saved = otpChallengeRepository.save(challenge);

        // Sent last: if the email fails, the transaction rolls back and no unusable code is left behind.
        emailService.sendVerificationCode(saved.getEmail(), saved.getName(), code, properties.validity());
        return saved;
    }

    private static String newCode() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    private static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
