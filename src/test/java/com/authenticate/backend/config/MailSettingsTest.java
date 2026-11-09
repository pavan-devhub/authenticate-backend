package com.authenticate.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** Checks how application.properties resolves the address the codes are sent from. */
class MailSettingsTest {

    @Test
    void theSenderIsTheGmailAccountWhenNothingElseIsSet() throws IOException {
        StandardEnvironment environment = environmentWith(Map.of("spring.mail.username", "sender@gmail.com"));

        assertThat(environment.getProperty("app.mail.from")).isEqualTo("sender@gmail.com");
    }

    @Test
    void theSenderFollowsWhicheverGmailAccountApplicationPropertiesEndsUpWith() throws IOException {
        StandardEnvironment environment = environmentWith(Map.of());

        assertThat(environment.getProperty("app.mail.from"))
                .isEqualTo(environment.getProperty("spring.mail.username"));
    }

    @Test
    void mailFromOverridesTheSender() throws IOException {
        StandardEnvironment environment = environmentWith(Map.of(
                "spring.mail.username", "sender@gmail.com",
                "MAIL_FROM", "alias@gmail.com"));

        assertThat(environment.getProperty("app.mail.from")).isEqualTo("alias@gmail.com");
    }

    // Loads application.properties with the given settings taking priority, the
    // way environment variables and .env do when the app runs.
    private static StandardEnvironment environmentWith(Map<String, Object> settings) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", settings));
        new PropertiesPropertySourceLoader()
                .load("application.properties", new ClassPathResource("application.properties"))
                .forEach(environment.getPropertySources()::addLast);
        return environment;
    }
}
