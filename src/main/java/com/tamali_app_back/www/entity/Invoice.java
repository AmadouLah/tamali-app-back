package com.tamali_app_back.www.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Invoice extends SyncableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", unique = true, nullable = false)
    private Sale sale;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    private String customerEmail;
    private String customerPhone;

    @Column(name = "receipt_pdf_url")
    private String receiptPdfUrl;

    private boolean sentByEmail;
    private boolean sentByWhatsapp;
}
