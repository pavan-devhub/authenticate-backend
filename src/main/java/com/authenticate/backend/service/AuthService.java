package com.authenticate.backend.service;

import com.authenticate.backend.dto.request.LoginRequest;
import com.authenticate.backend.dto.request.RegisterRequest;
import com.authenticate.backend.dto.request.ResendOtpRequest;
import com.authenticate.backend.dto.request.VerifyOtpRequest;
import com.authenticate.backend.dto.response.LoginResponse;
import com.authenticate.backend.dto.response.OtpChallengeResponse;
import com.authenticate.backend.dto.response.UserResponse;

public interface AuthService {

    /** Checks the details and emails a code. The account is only created by {@link #verifyRegistration}. */
    OtpChallengeResponse register(RegisterRequest request);

    /** Creates the account if the emailed code is correct. */
    UserResponse verifyRegistration(VerifyOtpRequest request);

    /** Emails a new code for a registration in progress. */
    OtpChallengeResponse resendCode(ResendOtpRequest request);

    /**
     * Checks the email and password and records the attempt in the user's login
     * history. Returns the user with their most recent login attempts, newest first.
     */
    LoginResponse login(LoginRequest request);
}
