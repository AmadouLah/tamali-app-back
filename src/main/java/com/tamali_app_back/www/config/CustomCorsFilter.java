package com.tamali_app_back.www.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtre CORS personnalisé qui s'exécute AVANT Spring Security
 * Ce filtre garantit que les en-têtes CORS sont TOUJOURS présents,
 * même si Spring Security ou d'autres filtres les bloquent
 */
@Component
@Slf4j
public class CustomCorsFilter extends OncePerRequestFilter {

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000}")
    private String allowedOriginsString;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String origin = request.getHeader("Origin");
        String method = request.getMethod();
        boolean isOptionsRequest = "OPTIONS".equalsIgnoreCase(method);
        
        // Log TOUJOURS pour voir toutes les requêtes
        // Log TOUJOURS pour voir toutes les requêtes, surtout OPTIONS
        log.info("🔵 CustomCorsFilter EXÉCUTÉ - Méthode: {}, Origin: {}, Path: {}", 
                method, origin, request.getRequestURI());
        
        if (isOptionsRequest) {
            log.info("🟢 REQUÊTE OPTIONS DÉTECTÉE - Traitement immédiat...");
        }
        
        // Parser les origines autorisées
        List<String> allowedOrigins = parseOrigins(allowedOriginsString);
        boolean isOriginAllowed = origin != null && originMatches(origin, allowedOrigins);
        
        // CRITIQUE: Pour les requêtes OPTIONS, TOUJOURS ajouter les en-têtes CORS et répondre immédiatement
        if (isOptionsRequest) {
            // Pour les requêtes OPTIONS, TOUJOURS utiliser l'origine de la requête si elle existe
            // Sinon utiliser la première origine autorisée ou "*"
            String responseOrigin = origin != null ? origin : 
                (!allowedOrigins.isEmpty() && !allowedOrigins.contains("*") ? allowedOrigins.get(0) : "*");
            
            // Toujours ajouter les en-têtes CORS pour les requêtes OPTIONS
            response.setHeader("Access-Control-Allow-Origin", responseOrigin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
            response.setHeader("Access-Control-Allow-Headers", 
                "Authorization, Content-Type, X-Requested-With, Accept, Origin, " +
                "Access-Control-Request-Method, Access-Control-Request-Headers, " +
                "X-Auth-Token, Cache-Control, Pragma, If-Modified-Since, If-None-Match, " +
                "X-Forwarded-For, X-Forwarded-Proto, X-Forwarded-Host, " +
                "sec-ch-ua, sec-ch-ua-mobile, sec-ch-ua-platform, " +
                "sec-fetch-dest, sec-fetch-mode, sec-fetch-site, sec-fetch-user");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Expose-Headers", 
                "Authorization, Content-Type, X-Auth-Token, " +
                "Access-Control-Allow-Origin, Access-Control-Allow-Credentials, " +
                "Access-Control-Allow-Methods, Access-Control-Allow-Headers");
            
            log.info("✅ Requête OPTIONS (preflight) traitée IMMÉDIATEMENT - En-têtes CORS ajoutés avec Origin: {}", responseOrigin);
            log.info("✅ Réponse HTTP 200 OK envoyée pour OPTIONS");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(0);
            response.flushBuffer();
            return; // Ne pas continuer la chaîne de filtres pour OPTIONS
        }
        
        // Pour les autres requêtes, ajouter les en-têtes CORS si l'origine est autorisée
        if (isOriginAllowed || origin == null) {
            String responseOrigin = origin != null ? origin : 
                (!allowedOrigins.isEmpty() && !allowedOrigins.contains("*") ? allowedOrigins.get(0) : "*");
            
            response.setHeader("Access-Control-Allow-Origin", responseOrigin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
            response.setHeader("Access-Control-Allow-Headers", 
                "Authorization, Content-Type, X-Requested-With, Accept, Origin, " +
                "Access-Control-Request-Method, Access-Control-Request-Headers, " +
                "X-Auth-Token, Cache-Control, Pragma, If-Modified-Since, If-None-Match, " +
                "X-Forwarded-For, X-Forwarded-Proto, X-Forwarded-Host, " +
                "sec-ch-ua, sec-ch-ua-mobile, sec-ch-ua-platform, " +
                "sec-fetch-dest, sec-fetch-mode, sec-fetch-site, sec-fetch-user");
            response.setHeader("Access-Control-Expose-Headers", 
                "Authorization, Content-Type, X-Auth-Token, " +
                "Access-Control-Allow-Origin, Access-Control-Allow-Credentials, " +
                "Access-Control-Allow-Methods, Access-Control-Allow-Headers");
            
            log.debug("En-têtes CORS ajoutés pour requête {} - Origin: {}", request.getMethod(), responseOrigin);
        }
        
        // Continuer avec la chaîne de filtres pour les autres requêtes
        filterChain.doFilter(request, response);
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
                        return origin.substring(0, origin.length() - 1);
                    }
                    return origin;
                })
                .filter(s -> !s.isEmpty())
                .toList();
        
        if (origins.isEmpty()) {
            log.warn("Aucune origine valide après parsing, utilisation de '*' par défaut");
            return List.of("*");
        }
        
        log.info("Origines CORS parsées dans CustomCorsFilter: {}", origins);
        return origins;
    }

    /** Vérifie si l'origine correspond à une des origines autorisées (exact ou pattern comme https://*.vercel.app) */
    private boolean originMatches(String origin, List<String> allowed) {
        if (allowed.contains("*")) return true;
        if (allowed.contains(origin)) return true;
        for (String pattern : allowed) {
            if (pattern.contains("*")) {
                if (matchesPattern(origin, pattern)) return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(String origin, String pattern) {
        int star = pattern.indexOf('*');
        if (star < 0) return origin.equals(pattern);
        String prefix = pattern.substring(0, star);
        String suffix = pattern.substring(star + 1);
        return origin.startsWith(prefix) && origin.endsWith(suffix) && origin.length() >= prefix.length() + suffix.length();
    }
}
