package com.authenticate.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record LoginRequest(
        @NotBlank(message = "Enter your email.")
        String email,

        @NotEmpty(message = "Enter your password.")
        String password) {

    // The generated record toString would print the password if this were ever logged.
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
