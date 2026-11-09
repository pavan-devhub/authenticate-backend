package com.authenticate.backend.dto.request;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your name.")
        @Size(max = 100, message = "Name must be at most 100 characters.")
        String name,

        @NotBlank(message = "Enter your email.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email must be at most 254 characters.")
        String email,

        @NotNull(message = "Enter a password.")
        @Size(min = 8, message = "Use at least 8 characters.")
        String password) {

    private static final int BCRYPT_MAX_BYTES = 72;

    @AssertTrue(message = "Password is too long. Use at most 72 characters.")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    // The generated record toString would print the password if this were ever logged.
    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", password=****]";
    }
}
