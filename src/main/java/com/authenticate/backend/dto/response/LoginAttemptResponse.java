package com.authenticate.backend.dto.response;

import java.time.Instant;

import com.authenticate.backend.entity.LoginOutcome;

public record LoginAttemptResponse(Long id, LoginOutcome outcome, boolean successful, Instant attemptedAt) {
}
