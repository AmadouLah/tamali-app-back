package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.ReceiptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptTemplateRepository extends JpaRepository<ReceiptTemplate, UUID> {

    Optional<ReceiptTemplate> findByCode(String code);

    List<ReceiptTemplate> findByActiveTrue();

    Optional<ReceiptTemplate> findByIsDefaultTrue();
}
