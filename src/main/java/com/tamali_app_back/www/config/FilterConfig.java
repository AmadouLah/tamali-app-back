package com.tamali_app_back.www.config;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration pour enregistrer le filtre CORS AVANT Spring Security
 * Ce filtre s'exécute avec la priorité la plus élevée pour garantir
 * que les requêtes OPTIONS sont traitées avant Spring Security
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class FilterConfig {

    private final CustomCorsFilter customCorsFilter;

    @Bean
    public FilterRegistrationBean<Filter> corsFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        // Utiliser le bean existant au lieu d'en créer un nouveau
        registration.setFilter(customCorsFilter);
        registration.addUrlPatterns("/*");
        // Utiliser une valeur négative très élevée pour être sûr d'être avant Spring Security
        // Spring Security utilise généralement -100, donc -1000 devrait être avant
        registration.setOrder(-10000);
        registration.setName("customCorsFilter");
        registration.setEnabled(true);
        log.info("Filtre CORS personnalisé enregistré avec priorité: {} (avant Spring Security)", registration.getOrder());
        return registration;
    }
}
