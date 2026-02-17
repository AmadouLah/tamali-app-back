package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank @Size(min = 8) String password,
        @NotBlank String firstname,
        @NotBlank String lastname
) {}
