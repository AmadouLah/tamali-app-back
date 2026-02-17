package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmLoginRequest(
        @NotNull UUID userId,
        @NotBlank String code
) {}
