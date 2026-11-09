package com.authenticate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "This code is no longer valid. Please start again.")
        String challengeId) {

    @Override
    public String toString() {
        return "ResendOtpRequest[challengeId=****]";
    }
}
