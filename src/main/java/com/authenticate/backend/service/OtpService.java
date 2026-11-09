package com.authenticate.backend.service;

import com.authenticate.backend.entity.OtpChallenge;
import com.authenticate.backend.entity.OtpPurpose;

/**
 * Creates, emails and checks one-time codes. Call these from inside a
 * transaction: AuthService decides what to keep when a check fails.
 */
public interface OtpService {

    /** Emails a code to confirm a new account. Replaces any earlier registration code for that email. */
    OtpChallenge startRegistration(String name, String email, String passwordHash);

    /** Emails a new code for a registration in progress. */
    OtpChallenge resend(String token);

    /**
     * Checks a code. A correct code uses up the challenge. A wrong one uses up
     * an attempt, and the last allowed attempt removes the challenge.
     */
    OtpCheckResult check(String token, OtpPurpose purpose, String code);
}
