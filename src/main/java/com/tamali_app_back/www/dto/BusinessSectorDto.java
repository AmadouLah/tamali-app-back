package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessSectorDto(
        UUID id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {}
