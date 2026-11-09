package com.authenticate.backend.mapper;

import com.authenticate.backend.dto.response.LoginAttemptResponse;
import com.authenticate.backend.entity.LoginAttempt;

public final class LoginAttemptMapper {

    private LoginAttemptMapper() {
    }

    public static LoginAttemptResponse toResponse(LoginAttempt attempt) {
        return new LoginAttemptResponse(
                attempt.getId(), attempt.getOutcome(), attempt.isSuccessful(), attempt.getAttemptedAt());
    }
}
