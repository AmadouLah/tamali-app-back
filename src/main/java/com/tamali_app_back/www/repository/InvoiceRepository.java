package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findBySaleId(UUID saleId);

    List<Invoice> findBySaleIdIn(List<UUID> saleIds);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
