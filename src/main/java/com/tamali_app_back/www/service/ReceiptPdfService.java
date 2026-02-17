package com.tamali_app_back.www.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.ReceiptTemplate;
import com.tamali_app_back.www.entity.Sale;
import com.tamali_app_back.www.entity.SaleItem;
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

        Map<String, String> variables = new HashMap<>();
        variables.put("${BUSINESS_LOGO}", logoHtml);
        variables.put("${BUSINESS_NAME}", business.getName() != null ? business.getName() : "");
        variables.put("${BUSINESS_EMAIL}", business.getEmail() != null ? business.getEmail() : "");
        variables.put("${BUSINESS_PHONE}", business.getPhone() != null ? business.getPhone() : "");
        variables.put("${BUSINESS_ADDRESS}", business.getAddress() != null ? business.getAddress() : "");
        variables.put("${SALE_DATE}", sale.getSaleDate().format(DATE_FORMATTER));
        variables.put("${SALE_ID}", sale.getId().toString());
        variables.put("${ITEMS}", buildItemsHtml(sale));
        variables.put("${SUBTOTAL}", formatMoney(sale.getTotalAmount().subtract(sale.getTaxAmount() != null ? sale.getTaxAmount() : BigDecimal.ZERO)));
        variables.put("${TAX}", formatMoney(sale.getTaxAmount() != null ? sale.getTaxAmount() : BigDecimal.ZERO));
        variables.put("${TOTAL}", formatMoney(sale.getTotalAmount()));

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            html = html.replace(entry.getKey(), entry.getValue());
        }

        return "<!DOCTYPE html><html><head><style>" + css + "</style></head><body>" + html + "</body></html>";
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
