package com.tamali_app_back.www.dto;

import java.util.UUID;

public record EmailCheckResponse(
        boolean exists,
        UUID userId,
        String email
) {}
