package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationDto(
        UUID id,
        String email,
        LocalDateTime expiresAt,
        boolean used,
        LocalDateTime createdAt
) {}
