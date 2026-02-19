package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.ProductCategoryDto;
import com.tamali_app_back.www.dto.request.ProductCategoryCreateRequest;
import com.tamali_app_back.www.dto.request.ProductCategoryUpdateRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.service.ProductCategoryService;
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
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping("/businesses/{businessId}/product-categories")
    public ResponseEntity<List<ProductCategoryDto>> findByBusinessId(@PathVariable UUID businessId) {
        return ResponseEntity.ok(productCategoryService.findByBusinessId(businessId));
    }

    @GetMapping("/product-categories/{id}")
    public ResponseEntity<ProductCategoryDto> getById(@PathVariable UUID id) {
        ProductCategoryDto dto = ResponseUtil.requireFound(productCategoryService.getById(id), "Catégorie", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/product-categories/{id}/products-count")
    public ResponseEntity<Long> getProductsCount(@PathVariable UUID id) {
        long count = productCategoryService.countProductsByCategoryId(id);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/businesses/{businessId}/product-categories")
    public ResponseEntity<ProductCategoryDto> create(
            @PathVariable UUID businessId,
            @Valid @RequestBody ProductCategoryCreateRequest request) {
        ProductCategoryDto dto = productCategoryService.create(businessId, request.name());
        if (dto == null) throw new BadRequestException("Business introuvable.");
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/product-categories/{id}")
    public ResponseEntity<ProductCategoryDto> update(@PathVariable UUID id, @Valid @RequestBody ProductCategoryUpdateRequest request) {
        ProductCategoryDto dto = ResponseUtil.requireFound(productCategoryService.update(id, request.name()), "Catégorie", id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/product-categories/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        productCategoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
