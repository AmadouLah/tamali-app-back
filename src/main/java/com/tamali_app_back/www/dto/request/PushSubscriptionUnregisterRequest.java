package com.tamali_app_back.www.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PushSubscriptionUnregisterRequest(@NotBlank String endpoint) {}
