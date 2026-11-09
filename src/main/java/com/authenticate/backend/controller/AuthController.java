package com.authenticate.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.authenticate.backend.dto.request.LoginRequest;
import com.authenticate.backend.dto.request.RegisterRequest;
import com.authenticate.backend.dto.request.ResendOtpRequest;
import com.authenticate.backend.dto.request.VerifyOtpRequest;
import com.authenticate.backend.dto.response.LoginResponse;
import com.authenticate.backend.dto.response.OtpChallengeResponse;
import com.authenticate.backend.dto.response.UserResponse;
import com.authenticate.backend.service.AuthService;

import jakarta.validation.Valid;

/**
 * Registering takes two steps: the first emails a one-time code, and the
 * second checks it and creates the account. Logging in takes one step.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpChallengeResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse verifyRegistration(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyRegistration(request);
    }

    @PostMapping("/otp/resend")
    public OtpChallengeResponse resendCode(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendCode(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
