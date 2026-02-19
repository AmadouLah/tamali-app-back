package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAssociateRequest(
        @NotBlank(message = "L'email est requis")
        @Email(message = "L'email doit être valide")
        String email
) {}
