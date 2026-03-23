package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByBusinessIdOrderByNameAsc(UUID businessId);

    Optional<Customer> findByIdAndBusinessId(UUID id, UUID businessId);

    Optional<Customer> findFirstByBusinessIdAndNameIgnoreCase(UUID businessId, String name);

    List<Customer> findByBusinessIdAndNameContainingIgnoreCaseOrderByNameAsc(UUID businessId, String q);
}
