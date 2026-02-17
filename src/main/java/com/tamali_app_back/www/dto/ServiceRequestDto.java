package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceRequestDto(
        UUID id,
        String email,
        String objective,
        boolean processed,
        LocalDateTime createdAt
) {}
