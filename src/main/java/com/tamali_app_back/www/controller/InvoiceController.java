package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.InvoiceDto;
import com.tamali_app_back.www.service.InvoiceService;
import com.tamali_app_back.www.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceDto> getById(@PathVariable UUID id) {
        InvoiceDto dto = ResponseUtil.requireFound(invoiceService.getById(id), "Facture", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/sales/{saleId}/invoice")
    public ResponseEntity<InvoiceDto> getBySaleId(@PathVariable UUID saleId) {
        InvoiceDto dto = ResponseUtil.requireFound(invoiceService.getBySaleId(saleId), "Facture pour cette vente", saleId);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/invoices/{id}/sent-email")
    public ResponseEntity<Void> markSentByEmail(@PathVariable UUID id) {
        invoiceService.markSentByEmail(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/invoices/{id}/sent-whatsapp")
    public ResponseEntity<Void> markSentByWhatsapp(@PathVariable UUID id) {
        invoiceService.markSentByWhatsapp(id);
        return ResponseEntity.noContent().build();
    }
}
