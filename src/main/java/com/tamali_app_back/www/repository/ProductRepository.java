package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByBusinessId(UUID businessId);
}
