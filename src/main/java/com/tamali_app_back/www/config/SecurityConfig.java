package com.tamali_app_back.www.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/actuator/health"
    };

    private static final String CSP_POLICY = "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'";
    private static final long HSTS_MAX_AGE = 31536000L;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        log.info("Configuration de la chaîne de sécurité Spring Security");
        log.info("Configuration CORS intégrée dans Spring Security");
        
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Configuration CORS - doit être avant authorizeHttpRequests
                .cors(cors -> {
                    cors.configurationSource(corsConfigurationSource);
                    log.info("CORS configuré dans Spring Security");
                })
                .headers(this::configureSecurityHeaders)
                .authorizeHttpRequests(this::configureAuthorizations)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    private void configureSecurityHeaders(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<?> headers) {
        headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                .frameOptions(frame -> frame.sameOrigin())
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(HSTS_MAX_AGE));
    }

    private void configureAuthorizations(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<?>.AuthorizationManagerRequestMatcherRegistry auth) {
        log.info("Configuration des autorisations Spring Security");
        log.info("Endpoints publics: {}", java.util.Arrays.toString(PUBLIC_ENDPOINTS));
        
        auth
                // CRITIQUE: Autoriser toutes les requêtes OPTIONS (preflight CORS) AVANT toute autre vérification
                // Cela garantit que les requêtes preflight CORS ne sont pas bloquées
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Autoriser les endpoints publics (inclut /api/service-requests)
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                // Toutes les autres requêtes nécessitent une authentification
                .anyRequest().authenticated();
        
        log.info("Configuration des autorisations terminée - OPTIONS et endpoints publics autorisés");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
