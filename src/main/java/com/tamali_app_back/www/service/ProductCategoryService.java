package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.ProductCategoryDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.ProductCategory;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final BusinessRepository businessRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<ProductCategoryDto> findByBusinessId(UUID businessId) {
        return productCategoryRepository.findByBusinessIdOrderByNameAsc(businessId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductCategoryDto getById(UUID id) {
        return productCategoryRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public ProductCategoryDto create(UUID businessId, String name) {
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) return null;
        ProductCategory cat = ProductCategory.builder()
                .name(name != null ? name.trim() : "")
                .business(business)
                .build();
        return toDto(productCategoryRepository.save(cat));
    }

    @Transactional
    public ProductCategoryDto update(UUID id, String name) {
        ProductCategory cat = productCategoryRepository.findById(id).orElse(null);
        if (cat == null) return null;
        if (name != null) cat.setName(name.trim());
        return toDto(productCategoryRepository.save(cat));
    }

    @Transactional
    public void deleteById(UUID id) {
        productCategoryRepository.findById(id).ifPresent(cat -> {
            cat.setDeletedAt(LocalDateTime.now());
            productCategoryRepository.save(cat);
        });
    }

    private ProductCategoryDto toDto(ProductCategory e) {
        if (e == null) return null;
        return new ProductCategoryDto(
                e.getId(),
                e.getName(),
                e.getBusiness() != null ? e.getBusiness().getId() : null
        );
    }
}
