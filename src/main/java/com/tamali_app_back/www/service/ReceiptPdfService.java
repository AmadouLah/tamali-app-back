package com.tamali_app_back.www.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.ReceiptTemplate;
import com.tamali_app_back.www.entity.Sale;
import com.tamali_app_back.www.entity.SaleItem;
import com.tamali_app_back.www.entity.TaxConfiguration;
import com.tamali_app_back.www.repository.TaxConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReceiptTemplateService receiptTemplateService;
    private final TaxConfigurationRepository taxConfigurationRepository;

    public byte[] generateReceiptPdf(Sale sale) {
        Business business = sale.getBusiness();
        ReceiptTemplate template = business.getReceiptTemplate();
        
        if (template == null) {
            template = receiptTemplateService.getRepository().findByIsDefaultTrue().orElse(null);
        }

        if (template == null) {
            throw new IllegalStateException("Aucun template de reçu disponible");
        }

        String html = buildReceiptHtml(template, sale, business);
        
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF du reçu", e);
            throw new RuntimeException("Impossible de générer le PDF du reçu", e);
        }
    }

    private String buildReceiptHtml(ReceiptTemplate template, Sale sale, Business business) {
        String html = template.getHtmlContent();
        String css = template.getCssContent() != null ? template.getCssContent() : "";

        String logoHtml = buildLogoHtml(business.getLogoUrl());

        TaxConfiguration taxConfig = taxConfigurationRepository.findByBusinessId(business.getId()).orElse(null);
        BigDecimal taxAmount = sale.getTaxAmount() != null ? sale.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = sale.getTotalAmount().subtract(taxAmount);
        
        String taxRateDisplay = "";
        boolean showTaxRate = taxConfig != null && taxConfig.isEnabled() && taxConfig.getRate() != null && taxConfig.getRate().compareTo(BigDecimal.ZERO) > 0;
        if (showTaxRate) {
            taxRateDisplay = String.format(" (%.0f%%)", taxConfig.getRate().doubleValue());
        }
        
        String taxLabel = "TVA" + taxRateDisplay + ":";
        String taxValue = formatMoney(taxAmount);

        log.debug("Génération reçu - Vente ID: {}, TVA configurée: {}, TVA activée: {}, Taux: {}, Montant TVA: {}, Sous-total: {}", 
                sale.getId(), 
                taxConfig != null, 
                taxConfig != null && taxConfig.isEnabled(),
                taxConfig != null && taxConfig.getRate() != null ? taxConfig.getRate() : "N/A",
                taxAmount,
                subtotal);

        Map<String, String> variables = new HashMap<>();
        variables.put("${BUSINESS_LOGO}", logoHtml);
        variables.put("${BUSINESS_NAME}", business.getName() != null ? business.getName() : "");
        variables.put("${BUSINESS_EMAIL}", business.getEmail() != null ? business.getEmail() : "");
        variables.put("${BUSINESS_PHONE}", business.getPhone() != null ? business.getPhone() : "");
        variables.put("${BUSINESS_ADDRESS}", business.getAddress() != null ? business.getAddress() : "");
        variables.put("${SALE_DATE}", sale.getSaleDate().format(DATE_FORMATTER));
        variables.put("${SALE_ID}", sale.getId().toString());
        variables.put("${ITEMS}", buildItemsHtml(sale));
        variables.put("${SUBTOTAL}", formatMoney(subtotal));
        variables.put("${TAX}", taxValue);
        variables.put("${TAX_LABEL}", taxLabel);
        variables.put("${TOTAL}", formatMoney(sale.getTotalAmount()));

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        String fullHtml = "<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + html + "</body></html>";
        return sanitizeForXhtml(fullHtml);
    }

    /** Rend le HTML compatible XHTML pour openhtmltopdf (balises vides auto-fermées). */
    private String sanitizeForXhtml(String html) {
        return html.replaceAll("<hr\\s*>", "<hr />")
                .replaceAll("<br\\s*>", "<br />")
                .replaceAll("<img([^>]*[^/])>", "<img$1 />");
    }

    private String buildLogoHtml(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return "";
        }
        try {
            byte[] imageBytes = RestClient.create().get()
                    .uri(logoUrl)
                    .retrieve()
                    .body(byte[].class);
            if (imageBytes != null && imageBytes.length > 0) {
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String mimeType = determineMimeType(logoUrl);
                return String.format("<img src=\"data:%s;base64,%s\" alt=\"Logo\" class=\"business-logo\" />", mimeType, base64Image);
            }
        } catch (Exception e) {
            log.warn("Impossible de charger le logo depuis l'URL: {}", logoUrl, e);
        }
        return "";
    }

    private String determineMimeType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".png")) return "image/png";
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) return "image/jpeg";
        if (lowerUrl.endsWith(".gif")) return "image/gif";
        if (lowerUrl.endsWith(".webp")) return "image/webp";
        return "image/png";
    }

    private String buildItemsHtml(Sale sale) {
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            return "<tr><td colspan='4'>Aucun article</td></tr>";
        }
        StringBuilder sb = new StringBuilder();
        for (SaleItem item : sale.getItems()) {
            String productName = item.getProduct() != null ? item.getProduct().getName() : "N/A";
            sb.append("<tr>")
                    .append("<td>").append(productName).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>").append(formatMoney(item.getPrice())).append("</td>")
                    .append("<td>").append(formatMoney(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).append("</td>")
                    .append("</tr>");
        }
        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0,00 FCFA";
        return String.format("%.2f FCFA", amount.doubleValue()).replace(".", ",");
    }
}
