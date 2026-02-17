package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BusinessCreateRequest(
        @NotBlank String name,
        String email,
        String phone,
        String address
) {}
