package com.authenticate.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.authenticate.backend.config.OtpProperties;
import com.authenticate.backend.entity.OtpChallenge;
import com.authenticate.backend.entity.OtpPurpose;
import com.authenticate.backend.exception.OtpExpiredException;
import com.authenticate.backend.exception.OtpResendTooSoonException;
import com.authenticate.backend.repository.OtpChallengeRepository;
import com.authenticate.backend.service.EmailService;
import com.authenticate.backend.service.OtpCheckResult;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    private static final Instant START = Instant.parse("2026-11-09T10:00:00Z");
    private static final Duration VALIDITY = Duration.ofMinutes(10);

    // A low cost factor keeps the real hashing fast in tests.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final MutableClock clock = new MutableClock(START);
    private final OtpProperties properties = new OtpProperties(VALIDITY, 5, Duration.ofSeconds(60));

    @Mock
    private OtpChallengeRepository repository;

    @Mock
    private EmailService emailService;

    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(repository, emailService, passwordEncoder, properties, clock);
    }

    @Test
    void startRegistrationEmailsASixDigitCodeAndStoresOnlyItsHash() {
        OtpChallenge challenge = startRegistration();
        String code = lastEmailedCode();

        assertThat(code).matches("\\d{6}");
        assertThat(challenge.getCodeHash()).isNotEqualTo(code);
        assertThat(passwordEncoder.matches(code, challenge.getCodeHash())).isTrue();
        assertThat(challenge.getExpiresAt()).isEqualTo(START.plus(VALIDITY));
        assertThat(challenge.getResendAvailableAt()).isEqualTo(START.plusSeconds(60));
        verify(repository).deleteByPurposeAndEmail(OtpPurpose.REGISTRATION, "ada@example.com");
        verify(emailService).sendVerificationCode(eq("ada@example.com"), eq("Ada"), eq(code), eq(VALIDITY));
    }

    @Test
    void eachChallengeGetsItsOwnUnguessableToken() {
        String first = startRegistration().getToken();
        String second = startRegistration().getToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    void theRightCodeUsesUpTheChallenge() {
        OtpChallenge challenge = startRegistration();
        when(repository.findByToken(challenge.getToken())).thenReturn(Optional.of(challenge));

        OtpCheckResult result = otpService.check(challenge.getToken(), OtpPurpose.REGISTRATION, lastEmailedCode());

        assertThat(result.correct()).isTrue();
        verify(repository).delete(challenge);
    }

    @Test
    void wrongCodesCountDownAndTheLastOneEndsTheChallenge() {
        OtpChallenge challenge = startRegistration();
        String wrongCode = wrongCodeFor(lastEmailedCode());
        when(repository.findByToken(challenge.getToken())).thenReturn(Optional.of(challenge));

        for (int attemptsLeft = 4; attemptsLeft >= 1; attemptsLeft--) {
            OtpCheckResult result = otpService.check(challenge.getToken(), OtpPurpose.REGISTRATION, wrongCode);
            assertThat(result.correct()).isFalse();
            assertThat(result.attemptsLeft()).isEqualTo(attemptsLeft);
        }
        verify(repository, never()).delete(any());

        OtpCheckResult last = otpService.check(challenge.getToken(), OtpPurpose.REGISTRATION, wrongCode);

        assertThat(last.attemptsLeft()).isZero();
        assertThat(last.failure()).isInstanceOf(OtpExpiredException.class)
                .hasMessage("Too many incorrect codes. Please start again.");
        verify(repository).delete(challenge);
    }

    @Test
    void evenTheRightCodeIsRejectedOnceItHasExpired() {
        OtpChallenge challenge = startRegistration();
        when(repository.findByToken(challenge.getToken())).thenReturn(Optional.of(challenge));
        clock.advance(VALIDITY);

        assertThatThrownBy(() -> otpService.check(challenge.getToken(), OtpPurpose.REGISTRATION, lastEmailedCode()))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessage("This code has expired. Please start again.");
        verify(repository).delete(challenge);
    }

    @Test
    void anUnknownTokenIsRejected() {
        when(repository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.check("unknown", OtpPurpose.REGISTRATION, "123456"))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessage("This code is no longer valid. Please start again.");
    }

    @Test
    void aNewCodeCannotBeSentDuringTheCooldown() {
        OtpChallenge challenge = startRegistration();
        when(repository.findByToken(challenge.getToken())).thenReturn(Optional.of(challenge));
        clock.advance(Duration.ofSeconds(10));

        assertThatThrownBy(() -> otpService.resend(challenge.getToken()))
                .isInstanceOf(OtpResendTooSoonException.class)
                .extracting(ex -> ((OtpResendTooSoonException) ex).getRetryAfterSeconds())
                .isEqualTo(50L);
    }

    @Test
    void aNewCodeReplacesTheOldOneButKeepsTheWrongAttempts() {
        OtpChallenge challenge = startRegistration();
        String oldCode = lastEmailedCode();
        when(repository.findByToken(challenge.getToken())).thenReturn(Optional.of(challenge));
        otpService.check(challenge.getToken(), OtpPurpose.REGISTRATION, wrongCodeFor(oldCode));
        clock.advance(Duration.ofSeconds(61));

        otpService.resend(challenge.getToken());
        String newCode = lastEmailedCode();

        assertThat(passwordEncoder.matches(newCode, challenge.getCodeHash())).isTrue();
        assertThat(challenge.getFailedAttempts()).isEqualTo(1);
        assertThat(challenge.getExpiresAt()).isEqualTo(START.plusSeconds(61).plus(VALIDITY));
    }

    private OtpChallenge startRegistration() {
        when(repository.save(any(OtpChallenge.class))).thenAnswer(call -> call.getArgument(0));
        return otpService.startRegistration("Ada", "ada@example.com", "password-hash");
    }

    private String lastEmailedCode() {
        ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendVerificationCode(any(), any(), codes.capture(), any());
        List<String> sent = codes.getAllValues();
        return sent.get(sent.size() - 1);
    }

    private static String wrongCodeFor(String code) {
        return code.equals("111111") ? "222222" : "111111";
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
