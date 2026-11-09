package com.authenticate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "This code is no longer valid. Please start again.")
        String challengeId,

        @NotBlank(message = "Enter the 6-digit code.")
        @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code.")
        String code) {

    // The generated record toString would print the code if this were ever logged.
    @Override
    public String toString() {
        return "VerifyOtpRequest[challengeId=****, code=****]";
    }
}
