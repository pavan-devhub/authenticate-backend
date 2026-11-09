package com.authenticate.backend.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Lets the app connect using a DATABASE_URL like
 * {@code postgresql://user:password@host:5432/database}, the form Render and
 * most hosts give out. Spring needs a JDBC URL with the username and password
 * as separate settings, so this splits it up before the app starts.
 *
 * <p>Registered in META-INF/spring.factories. Without DATABASE_URL it does nothing.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String DATABASE_URL = "DATABASE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty(DATABASE_URL);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        // First in line, so it wins over spring.datasource.* in application.properties.
        environment.getPropertySources()
                .addFirst(new MapPropertySource(DATABASE_URL, toDataSourceSettings(databaseUrl)));
    }

    static Map<String, Object> toDataSourceSettings(String databaseUrl) {
        URI uri = URI.create(databaseUrl.trim());
        String scheme = uri.getScheme();
        if (!"postgresql".equals(scheme) && !"postgres".equals(scheme)) {
            throw new IllegalArgumentException("DATABASE_URL must start with postgresql:// (got " + scheme + "://)");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("DATABASE_URL has no valid host name");
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("spring.datasource.url",
                "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query);

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            // Split before decoding, because the password itself may contain an encoded ':'.
            int colon = userInfo.indexOf(':');
            settings.put("spring.datasource.username", decode(colon < 0 ? userInfo : userInfo.substring(0, colon)));
            if (colon >= 0) {
                settings.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
        return settings;
    }

    // Percent-decoding only: in a URL's user part, '+' is a literal plus, not a space.
    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
