package com.tamali_app_back.www.service;

import com.tamali_app_back.www.entity.ReceiptTemplate;
import com.tamali_app_back.www.repository.ReceiptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class ReceiptTemplateInitializationService implements CommandLineRunner {

    private final ReceiptTemplateRepository receiptTemplateRepository;

    @Override
    public void run(String... args) {
        try {
            ensureReceiptTemplatesExist();
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des templates de reçus", e);
        }
    }

    private void ensureReceiptTemplatesExist() {
        if (receiptTemplateRepository.count() > 0) {
            log.info("Templates de reçus déjà existants.");
            return;
        }

        List<ReceiptTemplate> templates = Arrays.asList(
                createTemplate("classic", "Classique", getClassicHtml(), getClassicCss(), true),
                createTemplate("modern", "Moderne", getModernHtml(), getModernCss(), false),
                createTemplate("minimal", "Minimaliste", getMinimalHtml(), getMinimalCss(), false),
                createTemplate("elegant", "Élégant", getElegantHtml(), getElegantCss(), false),
                createTemplate("colorful", "Coloré", getColorfulHtml(), getColorfulCss(), false),
                createTemplate("professional", "Professionnel", getProfessionalHtml(), getProfessionalCss(), false)
        );

        receiptTemplateRepository.saveAll(templates);
        log.info("{} templates de reçus créés.", templates.size());
    }

    private ReceiptTemplate createTemplate(String code, String name, String html, String css, boolean isDefault) {
        return ReceiptTemplate.builder()
                .code(code)
                .name(name)
                .htmlContent(html)
                .cssContent(css)
                .isDefault(isDefault)
                .active(true)
                .build();
    }

    private String getClassicHtml() {
        return """
                <div class="receipt">
                    <div class="header-section">
                        ${BUSINESS_LOGO}
                        <h2>${BUSINESS_NAME}</h2>
                        <p>${BUSINESS_ADDRESS}</p>
                        <p>Tél: ${BUSINESS_PHONE} | Email: ${BUSINESS_EMAIL}</p>
                    </div>
                    <hr />
                    <p><strong>Reçu N°:</strong> ${SALE_ID}</p>
                    <p><strong>Date:</strong> ${SALE_DATE}</p>
                    <hr />
                    <table>
                        <thead>
                            <tr>
                                <th>Article</th>
                                <th>Qté</th>
                                <th>Prix</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${ITEMS}
                        </tbody>
                    </table>
                    <hr />
                    <p><strong>Sous-total:</strong> ${SUBTOTAL}</p>
                    <p><strong>TVA:</strong> ${TAX}</p>
                    <p><strong>Total:</strong> ${TOTAL}</p>
                    <hr />
                    <p class="footer">Merci de votre visite !</p>
                </div>
                """;
    }

    private String getClassicCss() {
        return """
                .receipt { font-family: Arial, sans-serif; padding: 20px; max-width: 300px; margin: 0 auto; }
                .header-section { text-align: center; margin-bottom: 15px; }
                .business-logo { max-width: 80px; max-height: 80px; margin-bottom: 10px; object-fit: contain; }
                h2 { text-align: center; margin-bottom: 10px; margin-top: 10px; }
                table { width: 100%; border-collapse: collapse; margin: 15px 0; }
                th, td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                .footer { text-align: center; margin-top: 20px; font-style: italic; }
                """;
    }

    private String getModernHtml() {
        return """
                <div class="receipt-modern">
                    <div class="header">
                        ${BUSINESS_LOGO}
                        <h1>${BUSINESS_NAME}</h1>
                        <p>${BUSINESS_ADDRESS}</p>
                    </div>
                    <div class="info">
                        <span>Reçu #${SALE_ID}</span>
                        <span>${SALE_DATE}</span>
                    </div>
                    <table class="items-table">
                        <thead>
                            <tr>
                                <th>Article</th>
                                <th>Qté</th>
                                <th>Prix</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>${ITEMS}</tbody>
                    </table>
                    <div class="totals">
                        <div>Sous-total: ${SUBTOTAL}</div>
                        <div>TVA: ${TAX}</div>
                        <div class="total">Total: ${TOTAL}</div>
                    </div>
                </div>
                """;
    }

    private String getModernCss() {
        return """
                .receipt-modern { font-family: 'Segoe UI', sans-serif; padding: 25px; max-width: 350px; }
                .header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 15px; margin-bottom: 15px; }
                .business-logo { max-width: 100px; max-height: 100px; margin-bottom: 15px; object-fit: contain; display: block; margin-left: auto; margin-right: auto; }
                .info { display: flex; justify-content: space-between; margin-bottom: 20px; }
                .items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                .items-table th, .items-table td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                .totals { border-top: 2px solid #333; padding-top: 15px; }
                .total { font-weight: bold; font-size: 1.2em; margin-top: 10px; }
                """;
    }

    private String getMinimalHtml() {
        return """
                <div class="receipt-minimal">
                    <div class="header-minimal">
                        ${BUSINESS_LOGO}
                        <h3>${BUSINESS_NAME}</h3>
                    </div>
                    <p>${SALE_DATE} | #${SALE_ID}</p>
                    <table>
                        <thead>
                            <tr>
                                <th>Article</th>
                                <th>Qté</th>
                                <th>Prix</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>${ITEMS}</tbody>
                    </table>
                    <div class="total">Total: ${TOTAL}</div>
                </div>
                """;
    }

    private String getMinimalCss() {
        return """
                .receipt-minimal { font-family: monospace; padding: 20px; max-width: 250px; }
                .header-minimal { text-align: center; margin-bottom: 15px; }
                .business-logo { max-width: 60px; max-height: 60px; margin-bottom: 8px; object-fit: contain; }
                h3 { margin-bottom: 10px; margin-top: 8px; }
                table { width: 100%; border-collapse: collapse; margin: 10px 0; }
                th, td { padding: 5px; text-align: left; border-bottom: 1px solid #000; }
                .total { font-weight: bold; margin-top: 15px; border-top: 1px solid #000; padding-top: 10px; }
                """;
    }

    private String getElegantHtml() {
        return """
                <div class="receipt-elegant">
                    <div class="header-elegant">
                        ${BUSINESS_LOGO}
                        <h2>${BUSINESS_NAME}</h2>
                        <p>${BUSINESS_ADDRESS}</p>
                        <p>${BUSINESS_PHONE}</p>
                    </div>
                    <div class="details">
                        <p>Reçu N° ${SALE_ID}</p>
                        <p>${SALE_DATE}</p>
                    </div>
                    <table class="items-elegant">
                        <thead>
                            <tr>
                                <th>Article</th>
                                <th>Qté</th>
                                <th>Prix</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>${ITEMS}</tbody>
                    </table>
                    <div class="summary">
                        <p>Sous-total: ${SUBTOTAL}</p>
                        <p>TVA: ${TAX}</p>
                        <p class="grand-total">Total: ${TOTAL}</p>
                    </div>
                </div>
                """;
    }

    private String getElegantCss() {
        return """
                .receipt-elegant { font-family: Georgia, serif; padding: 30px; max-width: 400px; }
                .header-elegant { text-align: center; border-bottom: 3px double #000; padding-bottom: 20px; }
                .business-logo { max-width: 90px; max-height: 90px; margin-bottom: 15px; object-fit: contain; }
                .items-elegant { width: 100%; border-collapse: collapse; margin: 20px 0; }
                .items-elegant th, .items-elegant td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
                .grand-total { font-size: 1.3em; font-weight: bold; margin-top: 15px; }
                """;
    }

    private String getColorfulHtml() {
        return """
                <div class="receipt-colorful">
                    <div class="header-color">
                        ${BUSINESS_LOGO}
                        <h2>${BUSINESS_NAME}</h2>
                    </div>
                    <div class="info-color">
                        <p>Reçu #${SALE_ID}</p>
                        <p>${SALE_DATE}</p>
                    </div>
                    <table class="items-color">
                        <thead>
                            <tr>
                                <th>Article</th>
                                <th>Qté</th>
                                <th>Prix</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>${ITEMS}</tbody>
                    </table>
                    <div class="totals-color">
                        <p>Sous-total: ${SUBTOTAL}</p>
                        <p>TVA: ${TAX}</p>
                        <p class="total-color">Total: ${TOTAL}</p>
                    </div>
                </div>
                """;
    }

    private String getColorfulCss() {
        return """
                .receipt-colorful { font-family: Arial, sans-serif; padding: 25px; max-width: 350px; }
                .header-color { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center; border-radius: 10px; }
                .business-logo { max-width: 100px; max-height: 100px; margin-bottom: 15px; object-fit: contain; background: white; padding: 8px; border-radius: 8px; }
                .items-color { width: 100%; border-collapse: collapse; margin: 20px 0; }
                .items-color th, .items-color td { padding: 8px; text-align: left; border-bottom: 1px solid #ddd; }
                .total-color { background: #f0f0f0; padding: 10px; font-weight: bold; border-radius: 5px; }
                """;
    }

    private String getProfessionalHtml() {
        return """
                <div class="receipt-professional">
                    <div class="company-info">
                        ${BUSINESS_LOGO}
                        <h2>${BUSINESS_NAME}</h2>
                        <p>${BUSINESS_ADDRESS}</p>
                        <p>${BUSINESS_PHONE} | ${BUSINESS_EMAIL}</p>
                    </div>
                    <div class="receipt-info">
                        <p><strong>Reçu N°:</strong> ${SALE_ID}</p>
                        <p><strong>Date:</strong> ${SALE_DATE}</p>
                    </div>
                    <table class="items-table">
                        <thead>
                            <tr>
                                <th>Description</th>
                                <th>Qté</th>
                                <th>Prix</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>${ITEMS}</tbody>
                    </table>
                    <div class="totals-professional">
                        <div class="row"><span>Sous-total:</span><span>${SUBTOTAL}</span></div>
                        <div class="row"><span>TVA:</span><span>${TAX}</span></div>
                        <div class="row total-row"><span>Total:</span><span>${TOTAL}</span></div>
                    </div>
                </div>
                """;
    }

    private String getProfessionalCss() {
        return """
                .receipt-professional { font-family: 'Helvetica Neue', Arial, sans-serif; padding: 30px; max-width: 450px; }
                .company-info { border-bottom: 2px solid #000; padding-bottom: 15px; margin-bottom: 20px; text-align: center; }
                .business-logo { max-width: 120px; max-height: 120px; margin-bottom: 15px; object-fit: contain; }
                .items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                .items-table th { background: #333; color: white; padding: 10px; text-align: left; }
                .items-table td { padding: 8px; border-bottom: 1px solid #ddd; }
                .totals-professional { margin-top: 20px; }
                .row { display: flex; justify-content: space-between; padding: 5px 0; }
                .total-row { font-weight: bold; font-size: 1.1em; border-top: 2px solid #000; padding-top: 10px; }
                """;
    }
}
