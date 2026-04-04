package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.InstantNotificationScope;
import com.tamali_app_back.www.enums.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InstantNotificationRequest(
        @NotBlank(message = "Le message est requis") String message,
        @NotNull(message = "La portée est requise") InstantNotificationScope scope,
        RoleType roleType,
        List<UUID> userIds
) {}
