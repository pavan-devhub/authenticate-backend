package com.authenticate.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.authenticate.backend.dto.request.LoginRequest;
import com.authenticate.backend.dto.request.RegisterRequest;
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
import com.authenticate.backend.repository.LoginAttemptRepository;
import com.authenticate.backend.repository.UserRepository;
import com.authenticate.backend.service.OtpCheckResult;
import com.authenticate.backend.service.OtpService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PASSWORD = "correct-password";
    private static final VerifyOtpRequest CODE = new VerifyOtpRequest("token-123", "123456");

    // A low cost factor keeps the real hashing fast in tests.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private OtpService otpService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, loginAttemptRepository, otpService, passwordEncoder);
    }

    @Test
    void registerEmailsACodeInsteadOfCreatingTheAccount() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(otpService.startRegistration(eq("Ada"), eq("ada@example.com"), anyString()))
                .thenReturn(withCode(pendingRegistration()));

        OtpChallengeResponse response = authService.register(
                new RegisterRequest("  Ada  ", "  Ada@Example.COM ", PASSWORD));

        ArgumentCaptor<String> passwordHash = ArgumentCaptor.forClass(String.class);
        verify(otpService).startRegistration(eq("Ada"), eq("ada@example.com"), passwordHash.capture());
        assertThat(passwordEncoder.matches(PASSWORD, passwordHash.getValue())).isTrue();
        assertThat(response.challengeId()).isEqualTo("token-123");
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Ada", "ada@example.com", PASSWORD)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(otpService);
    }

    @Test
    void theRightCodeCreatesTheAccountFromTheDetailsGivenAtRegistration() {
        when(otpService.check("token-123", OtpPurpose.REGISTRATION, "123456"))
                .thenReturn(new OtpCheckResult(pendingRegistration(), true, 0));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response = authService.verifyRegistration(CODE);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ada");
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("stored-hash");
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void aWrongCodeCreatesNoAccount() {
        when(otpService.check("token-123", OtpPurpose.REGISTRATION, "123456"))
                .thenReturn(new OtpCheckResult(pendingRegistration(), false, 3));

        assertThatThrownBy(() -> authService.verifyRegistration(CODE))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("That code is incorrect. You have 3 attempts left.");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void theLastWrongCodeMeansStartingAgain() {
        when(otpService.check("token-123", OtpPurpose.REGISTRATION, "123456"))
                .thenReturn(new OtpCheckResult(pendingRegistration(), false, 0));

        assertThatThrownBy(() -> authService.verifyRegistration(CODE))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessage("Too many incorrect codes. Please start again.");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void verifyRegistrationReportsAnEmailTakenInTheMeantime() {
        when(otpService.check("token-123", OtpPurpose.REGISTRATION, "123456"))
                .thenReturn(new OtpCheckResult(pendingRegistration(), true, 0));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() -> authService.verifyRegistration(CODE))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginWithTheRightPasswordRecordsASuccessAndReturnsTheHistory() {
        User user = storedUser();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(loginAttemptRepository.findByUserOrderByAttemptedAtDesc(eq(user), any(Limit.class)))
                .thenReturn(List.of(
                        new LoginAttempt(user, LoginOutcome.SUCCESS),
                        new LoginAttempt(user, LoginOutcome.WRONG_PASSWORD)));

        LoginResponse response = authService.login(new LoginRequest(" ADA@example.com ", PASSWORD));

        assertThat(recordedOutcome()).isEqualTo(LoginOutcome.SUCCESS);
        assertThat(response.user().email()).isEqualTo("ada@example.com");
        assertThat(response.loginAttempts())
                .extracting(LoginAttemptResponse::outcome)
                .containsExactly(LoginOutcome.SUCCESS, LoginOutcome.WRONG_PASSWORD);
    }

    @Test
    void loginSendsNoEmailedCode() {
        User user = storedUser();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        authService.login(new LoginRequest("ada@example.com", PASSWORD));

        verifyNoInteractions(otpService);
    }

    @Test
    void aWrongPasswordIsRecordedAndRejected() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(storedUser()));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Please enter the correct credentials.");

        assertThat(recordedOutcome()).isEqualTo(LoginOutcome.WRONG_PASSWORD);
    }

    @Test
    void anUnknownEmailGetsTheSameMessageAndNothingIsRecorded() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Please enter the correct credentials.");

        verify(loginAttemptRepository, never()).save(any());
    }

    private LoginOutcome recordedOutcome() {
        ArgumentCaptor<LoginAttempt> saved = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(saved.capture());
        return saved.getValue().getOutcome();
    }

    private static OtpChallenge pendingRegistration() {
        return OtpChallenge.forRegistration("token-123", "Ada", "ada@example.com", "stored-hash");
    }

    private static OtpChallenge withCode(OtpChallenge challenge) {
        Instant now = Instant.parse("2026-11-09T10:00:00Z");
        challenge.replaceCode("code-hash", now.plusSeconds(600), now.plusSeconds(60));
        return challenge;
    }

    private User storedUser() {
        return new User("Ada", "ada@example.com", passwordEncoder.encode(PASSWORD));
    }
}
