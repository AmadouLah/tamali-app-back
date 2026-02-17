package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.PaymentDto;
import com.tamali_app_back.www.service.PaymentService;
import com.tamali_app_back.www.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentDto> getById(@PathVariable UUID id) {
        PaymentDto dto = ResponseUtil.requireFound(paymentService.getById(id), "Paiement", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/sales/{saleId}/payment")
    public ResponseEntity<PaymentDto> getBySaleId(@PathVariable UUID saleId) {
        PaymentDto dto = ResponseUtil.requireFound(paymentService.getBySaleId(saleId), "Paiement pour cette vente", saleId);
        return ResponseEntity.ok(dto);
    }
}
