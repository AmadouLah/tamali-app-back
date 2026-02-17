package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.BusinessSector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessSectorRepository extends JpaRepository<BusinessSector, UUID> {
    List<BusinessSector> findByActiveTrue();
}
