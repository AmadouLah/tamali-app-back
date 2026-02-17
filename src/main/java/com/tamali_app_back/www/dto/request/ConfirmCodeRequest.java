package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmCodeRequest(@NotBlank String code) {}
