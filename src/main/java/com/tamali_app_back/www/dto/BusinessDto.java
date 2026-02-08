package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessDto(
        UUID id,
        String name,
        String email,
        String phone,
        String address,
        boolean active,
        LocalDateTime createdAt
) {}
