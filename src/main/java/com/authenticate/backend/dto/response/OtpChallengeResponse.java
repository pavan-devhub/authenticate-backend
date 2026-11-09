package com.authenticate.backend.dto.response;

import java.time.Instant;

/**
 * Sent when a code has been emailed. The client sends {@code challengeId}
 * back together with the code the user types in.
 */
public record OtpChallengeResponse(
        String challengeId,
        String email,
        Instant expiresAt,
        Instant resendAvailableAt) {
}
