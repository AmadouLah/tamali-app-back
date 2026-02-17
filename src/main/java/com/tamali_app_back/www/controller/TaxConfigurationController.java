package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.TaxConfigurationDto;
import com.tamali_app_back.www.dto.request.TaxConfigurationUpdateRequest;
import com.tamali_app_back.www.service.TaxConfigurationService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/tax-configuration")
@RequiredArgsConstructor
public class TaxConfigurationController {

    private final TaxConfigurationService taxConfigurationService;

    @GetMapping
    public ResponseEntity<TaxConfigurationDto> getByBusinessId(@PathVariable UUID businessId) {
        TaxConfigurationDto dto = ResponseUtil.requireFound(
                taxConfigurationService.getByBusinessId(businessId), "Configuration TVA du business", businessId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<TaxConfigurationDto> saveOrUpdate(
            @PathVariable UUID businessId,
            @Valid @RequestBody TaxConfigurationUpdateRequest request) {
        TaxConfigurationDto dto = ResponseUtil.requireFound(
                taxConfigurationService.saveOrUpdate(businessId, request.enabled(), request.rate()),
                "Business", businessId);
        return ResponseEntity.ok(dto);
    }
}
