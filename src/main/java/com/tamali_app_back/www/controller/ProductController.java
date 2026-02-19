package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.ProductDto;
import com.tamali_app_back.www.dto.StockMovementDto;
import com.tamali_app_back.www.dto.request.ProductCreateRequest;
import com.tamali_app_back.www.dto.request.ProductUpdateRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.service.ProductService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/businesses/{businessId}/products")
    public ResponseEntity<List<ProductDto>> findByBusinessId(@PathVariable UUID businessId) {
        return ResponseEntity.ok(productService.findByBusinessId(businessId));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable UUID id) {
        ProductDto dto = ResponseUtil.requireFound(productService.getById(id), "Produit", id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/businesses/{businessId}/products")
    public ResponseEntity<ProductDto> create(
            @PathVariable UUID businessId,
            @Valid @RequestBody ProductCreateRequest request) {
        ProductDto dto = productService.create(
                businessId, request.name(), request.reference(), request.categoryId(), request.unitPrice(),
                request.purchasePrice(), request.taxable(), request.initialQuantity());
        if (dto == null) throw new BadRequestException("Business introuvable.");
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ProductDto> update(@PathVariable UUID id, @RequestBody ProductUpdateRequest request) {
        ProductDto dto = ResponseUtil.requireFound(productService.update(id,
                request.name(), request.reference(), request.categoryId(), request.unitPrice(), request.purchasePrice(), request.taxable()), "Produit", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/products/{id}/movements")
    public ResponseEntity<List<StockMovementDto>> getMovements(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(productService.getMovements(id, limit));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
