package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.LegalStatus;

import java.util.UUID;

public record BusinessUpdateStepRequest(
        Integer step,
        String name,
        UUID sectorId,
        String address,
        String phone,
        String country,
        String commerceRegisterNumber,
        String identificationNumber,
        LegalStatus legalStatus,
        String bankAccountNumber,
        String websiteUrl,
        String logoUrl,
        UUID receiptTemplateId
) {}
