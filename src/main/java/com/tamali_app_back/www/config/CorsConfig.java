package com.tamali_app_back.www.config;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        
        log.info("=== Configuration CORS ===");
        log.info("Valeur brute de app.cors.allowed-origins: {}", allowedOriginsString);
        log.info("Origines autorisées parsées: {}", allowedOrigins);
        
        boolean isWildcard = allowedOrigins.contains("*");

        if (isWildcard) {
            log.info("Mode wildcard activé - toutes les origines autorisées");
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            log.info("Mode spécifique activé - origines: {}", allowedOrigins);
            // Utiliser setAllowedOrigins pour les origines spécifiques
            configuration.setAllowedOrigins(allowedOrigins);
            configuration.setAllowCredentials(true);
        }

        // Autoriser toutes les méthodes HTTP nécessaires
        configuration.setAllowedMethods(Arrays.asList(CORS_METHODS));
        log.info("Méthodes autorisées: {}", Arrays.toString(CORS_METHODS));
        
        // CORRECTION DÉFINITIVE: Utiliser setAllowedHeaders avec "*" pour autoriser tous les en-têtes
        // Cette approche fonctionne avec toutes les versions de Spring Framework
        configuration.setAllowedHeaders(Arrays.asList("*"));
        log.info("En-têtes autorisés: * (tous)");
        
        // Exposer les en-têtes de réponse importants
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type", 
                "X-Auth-Token",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Access-Control-Allow-Methods",
                "Access-Control-Allow-Headers"
        ));
        log.info("En-têtes exposés: Authorization, Content-Type, X-Auth-Token, Access-Control-*");
        
        // Cache des requêtes preflight pendant 1 heure
        configuration.setMaxAge(CORS_MAX_AGE);
        log.info("Max-Age (cache preflight): {} secondes", CORS_MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Appliquer la configuration CORS sur tous les endpoints
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("Configuration CORS appliquée avec succès sur /**");
        log.info("=== Fin Configuration CORS ===");
        return source;
    }

    /**
     * Filtre CORS supplémentaire pour garantir que les en-têtes CORS sont toujours présents
     * même si Spring Security ne les applique pas correctement
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsFilter filter = new CorsFilter(corsConfigurationSource());
        log.info("Filtre CORS créé et enregistré");
        return filter;
    }

    private List<String> parseOrigins(String originsString) {
        if (originsString == null || originsString.trim().isEmpty()) {
            log.warn("Aucune origine CORS configurée, utilisation de '*' par défaut");
            return List.of("*");
        }
        List<String> origins = Arrays.stream(originsString.split(","))
                .map(String::trim)
                .map(origin -> {
                    // Retirer le slash final s'il existe
                    if (origin.endsWith("/")) {
                        String cleaned = origin.substring(0, origin.length() - 1);
                        log.debug("Origine nettoyée: {} -> {}", origin, cleaned);
                        return cleaned;
                    }
                    return origin;
                })
                .filter(s -> !s.isEmpty())
                .toList();
        
        if (origins.isEmpty()) {
            log.warn("Aucune origine valide après parsing, utilisation de '*' par défaut");
            return List.of("*");
        }
        
        log.info("Origines CORS parsées avec succès: {}", origins);
        return origins;
    }
}
