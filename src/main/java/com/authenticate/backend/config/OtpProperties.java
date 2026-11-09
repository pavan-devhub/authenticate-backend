package com.authenticate.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** The {@code app.otp} settings in application.properties. */
@ConfigurationProperties("app.otp")
public record OtpProperties(
        @DefaultValue("10m") Duration validity,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("60s") Duration resendCooldown) {
}
