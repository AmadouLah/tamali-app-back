package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findByBusinessIdOrderBySaleDateDesc(UUID businessId, Pageable pageable);
}
