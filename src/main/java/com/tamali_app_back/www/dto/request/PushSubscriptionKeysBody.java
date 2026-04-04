package com.tamali_app_back.www.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PushSubscriptionKeysBody(
        @NotBlank String p256dh,
        @NotBlank String auth
) {}
