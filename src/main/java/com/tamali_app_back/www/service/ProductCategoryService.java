package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.ProductCategoryDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.Product;
import com.tamali_app_back.www.entity.ProductCategory;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.ProductCategoryRepository;
import com.tamali_app_back.www.repository.ProductRepository;
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
    private final ProductRepository productRepository;
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

    @Transactional(readOnly = true)
    public long countProductsByCategoryId(UUID categoryId) {
        return productRepository.countByCategoryId(categoryId);
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
            LocalDateTime now = LocalDateTime.now();
            // Supprimer tous les produits de cette catégorie
            List<Product> products = productRepository.findByCategoryId(id);
            for (Product product : products) {
                if (product.getDeletedAt() == null) {
                    product.setDeletedAt(now);
                    productRepository.save(product);
                }
            }
            // Supprimer la catégorie
            cat.setDeletedAt(now);
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
