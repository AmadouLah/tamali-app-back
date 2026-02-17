package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.StockDto;
import com.tamali_app_back.www.service.StockService;
import com.tamali_app_back.www.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/stocks/{id}")
    public ResponseEntity<StockDto> getById(@PathVariable UUID id) {
        StockDto dto = ResponseUtil.requireFound(stockService.getById(id), "Stock", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/products/{productId}/stock")
    public ResponseEntity<StockDto> getByProductId(@PathVariable UUID productId) {
        StockDto dto = ResponseUtil.requireFound(stockService.getByProductId(productId), "Stock du produit", productId);
        return ResponseEntity.ok(dto);
    }
}
