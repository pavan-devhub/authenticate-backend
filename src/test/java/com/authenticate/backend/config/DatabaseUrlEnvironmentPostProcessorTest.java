package com.authenticate.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void renderStyleUrlBecomesJdbcSettings() {
        Map<String, Object> settings = DatabaseUrlEnvironmentPostProcessor.toDataSourceSettings(
                "postgresql://authenticate:s3cret@dpg-abc123-a:5432/authenticate_db");

        assertThat(settings)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://dpg-abc123-a:5432/authenticate_db")
                .containsEntry("spring.datasource.username", "authenticate")
                .containsEntry("spring.datasource.password", "s3cret");
    }

    @Test
    void encodedCharactersInThePasswordAreDecoded() {
        Map<String, Object> settings = DatabaseUrlEnvironmentPostProcessor.toDataSourceSettings(
                "postgres://app:p%40ss%3Aw%2Frd+1@db.example.com/appdb?sslmode=require");

        assertThat(settings)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/appdb?sslmode=require")
                .containsEntry("spring.datasource.password", "p@ss:w/rd+1");
    }

    @Test
    void aNonPostgresUrlIsRejectedWithAClearMessage() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDataSourceSettings(
                "mysql://root:pw@localhost/db"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with postgresql://");
    }

    @Test
    void databaseUrlOverridesTheSettingsInApplicationProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("app", Map.of(
                "DATABASE_URL", "postgresql://u:p@host:6543/db",
                "spring.datasource.url", "jdbc:mysql://localhost:3306/authenticate_db")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:6543/db");
    }

    @Test
    void withoutDatabaseUrlNothingChanges() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("app", Map.of(
                "spring.datasource.url", "jdbc:mysql://localhost:3306/authenticate_db")));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:mysql://localhost:3306/authenticate_db");
    }
}
