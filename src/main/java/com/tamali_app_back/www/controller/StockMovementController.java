package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.StockMovementDto;
import com.tamali_app_back.www.dto.request.StockMovementCreateRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.service.StockMovementService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @GetMapping("/{id}")
    public ResponseEntity<StockMovementDto> getById(@PathVariable UUID productId, @PathVariable UUID id) {
        StockMovementDto dto = ResponseUtil.requireFound(stockMovementService.getById(id), "Mouvement de stock", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<StockMovementDto>> findByProductId(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(stockMovementService.findByProductId(productId, page, size));
    }

    @PostMapping
    public ResponseEntity<StockMovementDto> create(
            @PathVariable UUID productId,
            @Valid @RequestBody StockMovementCreateRequest request) {
        StockMovementDto dto = stockMovementService.create(productId, request.quantity(), request.type());
        if (dto == null)
            throw new BadRequestException("Produit ou stock introuvable, ou stock insuffisant pour une sortie.");
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
