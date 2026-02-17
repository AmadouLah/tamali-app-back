package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequestRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, max = 1000) String objective
) {}
