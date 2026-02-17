package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BusinessSectorCreateRequest(
        @NotBlank String name,
        String description
) {}
