package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findBySaleId(UUID saleId);

    List<Payment> findBySaleIdIn(List<UUID> saleIds);
}
