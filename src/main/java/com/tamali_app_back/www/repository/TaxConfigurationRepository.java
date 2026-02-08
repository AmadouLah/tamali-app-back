package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.TaxConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, UUID> {

    Optional<TaxConfiguration> findByBusinessId(UUID businessId);
}
