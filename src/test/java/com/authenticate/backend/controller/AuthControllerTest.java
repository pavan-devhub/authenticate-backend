package com.authenticate.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.authenticate.backend.dto.request.LoginRequest;
import com.authenticate.backend.dto.request.RegisterRequest;
import com.authenticate.backend.dto.request.ResendOtpRequest;
import com.authenticate.backend.dto.request.VerifyOtpRequest;
import com.authenticate.backend.dto.response.LoginAttemptResponse;
import com.authenticate.backend.dto.response.LoginResponse;
import com.authenticate.backend.dto.response.OtpChallengeResponse;
import com.authenticate.backend.dto.response.UserResponse;
import com.authenticate.backend.entity.LoginOutcome;
import com.authenticate.backend.exception.EmailAlreadyExistsException;
import com.authenticate.backend.exception.InvalidCredentialsException;
import com.authenticate.backend.exception.InvalidOtpException;
import com.authenticate.backend.exception.OtpExpiredException;
import com.authenticate.backend.exception.OtpResendTooSoonException;
import com.authenticate.backend.service.AuthService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String VALID_REGISTRATION = """
            {"name": "Ada", "email": "ada@example.com", "password": "correct-password"}
            """;
    private static final String VALID_LOGIN = """
            {"email": "ada@example.com", "password": "correct-password"}
            """;
    private static final String VALID_CODE = """
            {"challengeId": "token-123", "code": "123456"}
            """;
    private static final OtpChallengeResponse CHALLENGE = new OtpChallengeResponse(
            "token-123",
            "ada@example.com",
            Instant.parse("2026-11-09T10:10:00Z"),
            Instant.parse("2026-11-09T10:01:00Z"));
    private static final UserResponse ADA = new UserResponse(1L, "Ada", "ada@example.com");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerEmailsACodeAndReturnsTheChallenge() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(CHALLENGE);

        postJson("/api/auth/register", VALID_REGISTRATION)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.challengeId").value("token-123"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-09T10:10:00Z"))
                .andExpect(jsonPath("$.resendAvailableAt").value("2026-11-09T10:01:00Z"));
    }

    @Test
    void registerRejectsInvalidDetailsBeforeReachingTheService() throws Exception {
        postJson("/api/auth/register", """
                {"name": "  ", "email": "not-an-email", "password": "short"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Enter your name."))
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address."))
                .andExpect(jsonPath("$.fieldErrors.password").value("Use at least 8 characters."));

        verifyNoInteractions(authService);
    }

    @Test
    void registerRejectsAPasswordLongerThanBcryptCanHash() throws Exception {
        String tooManyBytes = "é".repeat(40);

        postJson("/api/auth/register", """
                {"name": "Ada", "email": "ada@example.com", "password": "%s"}
                """.formatted(tooManyBytes))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Password is too long. Use at most 72 characters."));

        verifyNoInteractions(authService);
    }

    @Test
    void registerReportsAnEmailThatIsAlreadyTaken() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException());

        postJson("/api/auth/register", VALID_REGISTRATION)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."));
    }

    @Test
    void anEmailThatCannotBeSentIsReportedWithoutTechnicalDetails() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new MailSendException("535 Username and Password not accepted"));

        postJson("/api/auth/register", VALID_REGISTRATION)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail")
                        .value("We couldn't send the verification email. Please try again in a moment."));
    }

    @Test
    void theRightRegistrationCodeReturnsTheNewUser() throws Exception {
        when(authService.verifyRegistration(any(VerifyOtpRequest.class))).thenReturn(ADA);

        postJson("/api/auth/register/verify", VALID_CODE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void somethingOtherThanSixDigitsIsRejectedBeforeReachingTheService() throws Exception {
        postJson("/api/auth/register/verify", """
                {"challengeId": "token-123", "code": "12ab"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").value("Enter the 6-digit code."));

        verifyNoInteractions(authService);
    }

    @Test
    void aWrongCodeSaysHowManyAttemptsAreLeft() throws Exception {
        when(authService.verifyRegistration(any(VerifyOtpRequest.class))).thenThrow(new InvalidOtpException(2));

        postJson("/api/auth/register/verify", VALID_CODE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That code is incorrect. You have 2 attempts left."))
                .andExpect(jsonPath("$.attemptsLeft").value(2));
    }

    @Test
    void anExpiredCodeTellsTheUserToStartAgain() throws Exception {
        when(authService.verifyRegistration(any(VerifyOtpRequest.class))).thenThrow(OtpExpiredException.expired());

        postJson("/api/auth/register/verify", VALID_CODE)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("This code has expired. Please start again."));
    }

    @Test
    void loginReportsWrongCredentialsWithTheMessageShownToTheUser() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        postJson("/api/auth/login", VALID_LOGIN)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please enter the correct credentials."));
    }

    @Test
    void theRightPasswordLogsInAndReturnsTheLoginHistory() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new LoginResponse(ADA, List.of(
                new LoginAttemptResponse(9L, LoginOutcome.SUCCESS, true, Instant.parse("2026-11-09T10:02:00Z")),
                new LoginAttemptResponse(8L, LoginOutcome.WRONG_PASSWORD, false, Instant.parse("2026-11-09T10:01:30Z")))));

        postJson("/api/auth/login", VALID_LOGIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.loginAttempts[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.loginAttempts[0].successful").value(true))
                .andExpect(jsonPath("$.loginAttempts[0].attemptedAt").value("2026-11-09T10:02:00Z"))
                .andExpect(jsonPath("$.loginAttempts[1].outcome").value("WRONG_PASSWORD"));
    }

    @Test
    void loginNoLongerHasACodeStep() throws Exception {
        postJson("/api/auth/login/verify", VALID_CODE).andExpect(status().isNotFound());
    }

    @Test
    void resendReturnsTheUpdatedChallenge() throws Exception {
        when(authService.resendCode(any(ResendOtpRequest.class))).thenReturn(CHALLENGE);

        postJson("/api/auth/otp/resend", """
                {"challengeId": "token-123"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeId").value("token-123"));
    }

    @Test
    void aNewCodeRequestedTooSoonSaysHowLongToWait() throws Exception {
        when(authService.resendCode(any(ResendOtpRequest.class))).thenThrow(new OtpResendTooSoonException(42));

        postJson("/api/auth/otp/resend", """
                {"challengeId": "token-123"}
                """)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "42"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(42))
                .andExpect(jsonPath("$.detail").value("Please wait 42 seconds before asking for a new code."));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        postJson("/api/auth/login", "{not json").andExpect(status().isBadRequest());
    }

    @Test
    void theFrontendOriginPassesTheCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void theDeployedFrontendOnVercelPassesTheCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://authenticate-frontend.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://authenticate-frontend.vercel.app"));
    }

    @Test
    void aDevServerOnAnotherLocalhostPortPassesTheCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5177")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5177"));
    }

    @Test
    void otherOriginsFailTheCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
