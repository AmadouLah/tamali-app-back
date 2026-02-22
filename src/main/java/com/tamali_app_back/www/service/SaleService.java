package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.InvoiceDto;
import com.tamali_app_back.www.dto.PaymentDto;
import com.tamali_app_back.www.dto.SaleDto;
import com.tamali_app_back.www.dto.request.SaleItemRequest;
import lombok.extern.slf4j.Slf4j;
import com.tamali_app_back.www.entity.*;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.exception.ResourceNotFoundException;
import com.tamali_app_back.www.enums.MovementType;
import com.tamali_app_back.www.enums.PaymentMethod;
import com.tamali_app_back.www.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final TaxConfigurationRepository taxConfigurationRepository;
    private final EntityMapper mapper;
    private final ReceiptPdfService receiptPdfService;
    private final SupabaseStorageService supabaseStorage;

    @Transactional(readOnly = true)
    public List<SaleDto> findByBusinessId(UUID businessId, int page, int size) {
        return saleRepository.findByBusinessIdOrderBySaleDateDesc(businessId, PageRequest.of(page, size))
                .stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SaleDto getById(UUID id) {
        return saleRepository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentBySaleId(UUID saleId) {
        return paymentService.getBySaleId(saleId).orElse(null);
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceBySaleId(UUID saleId) {
        return invoiceService.getBySaleId(saleId).orElse(null);
    }

    @Transactional
    public SaleDto createSale(UUID businessId, UUID cashierId, List<SaleItemRequest> items, PaymentMethod method,
                             String customerEmail, String customerPhone) {
        Business business = businessRepository.findById(businessId).orElse(null);
        User cashier = userRepository.findById(cashierId).orElse(null);
        if (business == null || cashier == null || items == null || items.isEmpty()) return null;

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        TaxConfiguration taxConfig = taxConfigurationRepository.findByBusinessId(businessId).orElse(null);
        // Taux TVA : config business si activée, sinon 18% pour produits taxables (cohérent avec le frontend)
        BigDecimal taxRate = (taxConfig != null && taxConfig.isEnabled() && taxConfig.getRate() != null && taxConfig.getRate().compareTo(BigDecimal.ZERO) > 0)
                ? taxConfig.getRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : new BigDecimal("0.18");

        BigDecimal onePlusTaxRate = BigDecimal.ONE.add(taxRate);
        List<SaleItem> entityItems = new ArrayList<>();
        for (SaleItemRequest req : items) {
            Product product = productRepository.findById(req.productId()).orElse(null);
            if (product == null) continue;
            Stock stock = stockRepository.findByProductId(product.getId()).orElse(null);
            if (stock == null || stock.getQuantity() < req.quantity()) return null;

            BigDecimal price = product.getUnitPrice();
            BigDecimal itemLineTotal = price.multiply(BigDecimal.valueOf(req.quantity()));
            BigDecimal itemTax = BigDecimal.ZERO;
            BigDecimal itemTotalTTC = itemLineTotal;
            if (product.isTaxable()) {
                // Produit taxable : prix stocké = TTC, on extrait HT et TVA
                BigDecimal itemHT = itemLineTotal.divide(onePlusTaxRate, 4, RoundingMode.HALF_UP);
                itemTax = itemLineTotal.subtract(itemHT).setScale(4, RoundingMode.HALF_UP);
            }
            totalAmount = totalAmount.add(itemTotalTTC);
            taxAmount = taxAmount.add(itemTax);

            SaleItem si = SaleItem.builder()
                    .product(product)
                    .quantity(req.quantity())
                    .price(price)
                    .build();
            entityItems.add(si);
        }

        Sale sale = Sale.builder()
                .business(business)
                .cashier(cashier)
                .items(entityItems)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .saleDate(LocalDateTime.now())
                .build();
        for (SaleItem si : entityItems) si.setSale(sale);
        sale = saleRepository.save(sale);
        
        log.debug("Vente créée - ID: {}, Total: {}, TVA: {}, Taux: {}",
                sale.getId(), totalAmount, taxAmount, taxRate);

        for (SaleItem si : sale.getItems()) {
            Stock stock = stockRepository.findByProductIdForUpdate(si.getProduct().getId()).orElseThrow();
            if (stock.getQuantity() < si.getQuantity()) {
                throw new BadRequestException(
                        "Stock insuffisant pour le produit " + si.getProduct().getName());
            }
            stock.setQuantity(stock.getQuantity() - si.getQuantity());
            stockRepository.save(stock);
            StockMovement mov = StockMovement.builder()
                    .product(si.getProduct())
                    .quantity(-si.getQuantity())
                    .type(MovementType.SALE)
                    .movementAt(LocalDateTime.now())
                    .build();
            stockMovementRepository.save(mov);
        }

        Payment payment = Payment.builder()
                .sale(sale)
                .amount(totalAmount)
                .method(method != null ? method : PaymentMethod.CASH)
                .paymentDate(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        String invoiceNumber = "INV-" + sale.getId().toString().replace("-", "").toUpperCase();
        Invoice invoice = Invoice.builder()
                .sale(sale)
                .invoiceNumber(invoiceNumber)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .sentByEmail(false)
                .sentByWhatsapp(false)
                .build();
        invoiceRepository.save(invoice);

        uploadReceiptToSupabase(sale, invoice);

        return mapper.toDto(sale);
    }

    @Transactional
    public void markInvoiceSentByEmail(UUID invoiceId) {
        invoiceService.markSentByEmail(invoiceId);
    }

    @Transactional
    public void markInvoiceSentByWhatsapp(UUID invoiceId) {
        invoiceService.markSentByWhatsapp(invoiceId);
    }

    /**
     * Génère le PDF du reçu avec le template choisi par le propriétaire, l'upload vers Supabase et retourne l'URL.
     */
    @Transactional
    public String generateAndUploadReceipt(UUID saleId) {
        Sale sale = saleRepository.findByIdWithBusinessAndTemplate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", saleId));
        Invoice invoice = invoiceRepository.findBySaleId(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture pour cette vente", saleId));

        byte[] pdfBytes = receiptPdfService.generateReceiptPdf(sale);
        String receiptUrl = supabaseStorage.uploadReceiptPdf(sale.getBusiness().getId(), sale.getId(), pdfBytes);
        invoice.setReceiptPdfUrl(receiptUrl);
        invoiceRepository.save(invoice);
        return receiptUrl;
    }

    private void uploadReceiptToSupabase(Sale sale, Invoice invoice) {
        try {
            byte[] pdfBytes = receiptPdfService.generateReceiptPdf(sale);
            String receiptUrl = supabaseStorage.uploadReceiptPdf(sale.getBusiness().getId(), sale.getId(), pdfBytes);
            invoice.setReceiptPdfUrl(receiptUrl);
        } catch (Exception e) {
            log.warn("Impossible d'uploader le reçu vers Supabase pour la vente {}: {}", sale.getId(), e.getMessage());
        }
    }
}
