package com.tamali_app_back.www.entity;

import com.tamali_app_back.www.enums.LegalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Business extends SyncableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String phone;
    private String address;
    private String country;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private BusinessSector sector;
    
    @Column(name = "commerce_register_number")
    private String commerceRegisterNumber;
    
    @Column(name = "identification_number")
    private String identificationNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status")
    private LegalStatus legalStatus;
    
    @Column(name = "bank_account_number")
    private String bankAccountNumber;
    
    @Column(name = "website_url")
    private String websiteUrl;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Sale> sales = new ArrayList<>();

    @OneToOne(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private TaxConfiguration taxConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_template_id")
    private ReceiptTemplate receiptTemplate;

    @Column(name = "logo_url")
    private String logoUrl;

    @Builder.Default
    private boolean active = true;
}
