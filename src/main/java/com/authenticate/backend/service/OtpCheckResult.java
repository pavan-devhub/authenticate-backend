package com.authenticate.backend.service;

import com.authenticate.backend.entity.OtpChallenge;
import com.authenticate.backend.exception.InvalidOtpException;
import com.authenticate.backend.exception.OtpExpiredException;

/** The result of checking a code that the user typed in. */
public record OtpCheckResult(OtpChallenge challenge, boolean correct, int attemptsLeft) {

    /** The exception that reports a wrong code to the user. */
    public RuntimeException failure() {
        return attemptsLeft > 0 ? new InvalidOtpException(attemptsLeft) : OtpExpiredException.tooManyAttempts();
    }
}
