package com.tamali_app_back.www.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final String[] CORS_METHODS = {
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
    };

    private static final long CORS_MAX_AGE = 3600L;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000}")
    private String allowedOriginsString;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = parseOrigins(allowedOriginsString);
        boolean isWildcard = allowedOrigins.contains("*");

        if (isWildcard) {
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            configuration.setAllowedOrigins(allowedOrigins);
            configuration.setAllowCredentials(true);
        }

        configuration.setAllowedMethods(Arrays.asList(CORS_METHODS));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Auth-Token"));
        configuration.setMaxAge(CORS_MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    private List<String> parseOrigins(String originsString) {
        if (originsString == null || originsString.trim().isEmpty()) {
            return List.of("*");
        }
        return Arrays.stream(originsString.split(","))
                .map(String::trim)
                .map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
