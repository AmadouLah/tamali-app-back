package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.ReceiptTemplateDto;
import com.tamali_app_back.www.service.ReceiptTemplateService;
import com.tamali_app_back.www.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receipt-templates")
@RequiredArgsConstructor
public class ReceiptTemplateController {

    private final ReceiptTemplateService receiptTemplateService;

    @GetMapping
    public ResponseEntity<List<ReceiptTemplateDto>> findAllActive() {
        return ResponseEntity.ok(receiptTemplateService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptTemplateDto> getById(@PathVariable UUID id) {
        ReceiptTemplateDto dto = ResponseUtil.requireFound(
                receiptTemplateService.getById(id), "ReceiptTemplate", id);
        return ResponseEntity.ok(dto);
    }
}
