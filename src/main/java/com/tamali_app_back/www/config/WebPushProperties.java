package com.tamali_app_back.www.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webpush")
public record WebPushProperties(
        String vapidPublicKey,
        String vapidPrivateKey,
        String subject
) {
    public WebPushProperties {
        if (vapidPublicKey == null) vapidPublicKey = "";
        if (vapidPrivateKey == null) vapidPrivateKey = "";
        if (subject == null || subject.isBlank()) {
            subject = "mailto:support@tamali.app";
        }
    }

    public boolean isConfigured() {
        return !vapidPublicKey.isBlank() && !vapidPrivateKey.isBlank();
    }
}
