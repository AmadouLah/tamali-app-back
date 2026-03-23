package com.tamali_app_back.www.dto;

import java.util.UUID;

public record CustomerDto(
        UUID id,
        UUID businessId,
        String name,
        String phone
) {}
