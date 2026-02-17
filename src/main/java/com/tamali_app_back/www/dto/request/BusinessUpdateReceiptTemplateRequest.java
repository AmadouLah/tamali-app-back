package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BusinessUpdateReceiptTemplateRequest(@NotNull UUID receiptTemplateId) {}
