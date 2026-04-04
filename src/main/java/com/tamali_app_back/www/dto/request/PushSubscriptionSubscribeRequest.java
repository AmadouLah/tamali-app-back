package com.tamali_app_back.www.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PushSubscriptionSubscribeRequest(
        @NotBlank String endpoint,
        @NotNull @Valid PushSubscriptionKeysBody keys
) {}
