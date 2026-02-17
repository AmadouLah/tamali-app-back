package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckEmailRequest(@NotBlank @Email String email) {}
