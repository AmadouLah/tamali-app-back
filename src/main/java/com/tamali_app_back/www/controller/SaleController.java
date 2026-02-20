package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.InvoiceDto;
import com.tamali_app_back.www.dto.PaymentDto;
import com.tamali_app_back.www.dto.SaleDto;
import com.tamali_app_back.www.dto.request.SaleCreateRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.service.SaleService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping("/businesses/{businessId}/sales")
    public ResponseEntity<List<SaleDto>> findByBusinessId(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(saleService.findByBusinessId(businessId, page, size));
    }

    @GetMapping("/sales/{id}")
    public ResponseEntity<SaleDto> getById(@PathVariable UUID id) {
        SaleDto dto = ResponseUtil.requireFound(saleService.getById(id), "Vente", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/sales/{id}/payment")
    public ResponseEntity<PaymentDto> getPaymentBySaleId(@PathVariable UUID id) {
        PaymentDto dto = ResponseUtil.requireFound(
                saleService.getPaymentBySaleId(id), "Paiement pour cette vente", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/sales/{id}/invoice")
    public ResponseEntity<InvoiceDto> getInvoiceBySaleId(@PathVariable UUID id) {
        InvoiceDto dto = ResponseUtil.requireFound(
                saleService.getInvoiceBySaleId(id), "Facture pour cette vente", id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/sales/{id}/generate-receipt")
    public ResponseEntity<Map<String, String>> generateReceipt(@PathVariable UUID id) {
        String receiptPdfUrl = saleService.generateAndUploadReceipt(id);
        return ResponseEntity.ok(Map.of("receiptPdfUrl", receiptPdfUrl));
    }

    @PostMapping("/businesses/{businessId}/sales")
    public ResponseEntity<SaleDto> create(
            @PathVariable UUID businessId,
            @Valid @RequestBody SaleCreateRequest request) {
        SaleDto dto = saleService.createSale(
                businessId, request.cashierId(), request.items(), request.method(),
                request.customerEmail(), request.customerPhone());
        if (dto == null)
            throw new BadRequestException("Données invalides : business ou caissier introuvable, ou stock insuffisant.");
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
