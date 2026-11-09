package com.authenticate.backend.mapper;

import com.authenticate.backend.dto.response.OtpChallengeResponse;
import com.authenticate.backend.entity.OtpChallenge;

public final class OtpChallengeMapper {

    private OtpChallengeMapper() {
    }

    public static OtpChallengeResponse toResponse(OtpChallenge challenge) {
        return new OtpChallengeResponse(
                challenge.getToken(),
                challenge.getEmail(),
                challenge.getExpiresAt(),
                challenge.getResendAvailableAt());
    }
}
