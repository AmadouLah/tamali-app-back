package com.tamali_app_back.www.dto;

import java.util.UUID;

public record NotificationUserOptionDto(
        UUID id,
        String email,
        String displayName,
        String roleType
) {}
