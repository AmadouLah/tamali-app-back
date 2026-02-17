package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DirectPasswordLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
