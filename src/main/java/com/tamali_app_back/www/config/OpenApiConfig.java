package com.tamali_app_back.www.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tamali API")
                        .description("API de comptabilité et gestion de stock pour les entreprises (Mali)")
                        .version("1.0"));
    }
}
