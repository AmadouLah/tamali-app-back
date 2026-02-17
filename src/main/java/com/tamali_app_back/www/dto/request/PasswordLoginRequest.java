package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PasswordLoginRequest(
        UUID userId,
        @NotBlank String password
) {}
