package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.ProductCategory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    List<ProductCategory> findByBusinessIdOrderByNameAsc(UUID businessId);
}
