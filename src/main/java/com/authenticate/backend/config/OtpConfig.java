package com.authenticate.backend.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OtpProperties.class)
public class OtpConfig {

    // Injected instead of calling Instant.now(), so tests can control the time.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
