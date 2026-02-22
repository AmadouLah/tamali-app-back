package com.tamali_app_back.www.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
@Slf4j
public class BrevoMailService implements MailService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SUBJECT_CODE = "Votre code de connexion — Tamali";
    private static final String SUBJECT_INVITATION = "Invitation à rejoindre Tamali";
    private static final String SUBJECT_SERVICE_REQUEST = "Nouvelle demande d'utilisation — Tamali";
    private static final String SUBJECT_TEMPORARY_PASSWORD = "Vos identifiants Tamali";

    private final RestClient restClient;
    private final String fromEmail;
    private final String fromName;

    public BrevoMailService(
            @Value("${app.mail.brevo.api-key}") String apiKey,
            @Value("${app.mail.from}") String fromEmail) {
        this.restClient = RestClient.builder()
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.fromEmail = fromEmail;
        this.fromName = "Tamali";
    }

    @Override
    public void sendVerificationCode(String toEmail, String code, int validityMinutes) {
        String html = VerificationEmailTemplate.buildHtml(code, validityMinutes);
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", toEmail)),
                "subject", SUBJECT_CODE,
                "htmlContent", html
        );
        try {
            restClient.post()
                    .uri(BREVO_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Code envoyé à {}", toEmail);
        } catch (Exception e) {
            log.error("Échec envoi email Brevo vers {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email de vérification.", e);
        }
    }

    @Override
    public void sendInvitation(String toEmail, String invitationLink, int validityDays) {
        String html = InvitationEmailTemplate.buildHtml(invitationLink, validityDays);
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", toEmail)),
                "subject", SUBJECT_INVITATION,
                "htmlContent", html
        );
        try {
            restClient.post()
                    .uri(BREVO_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Invitation envoyée à {}", toEmail);
        } catch (Exception e) {
            log.error("Échec envoi invitation Brevo vers {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email d'invitation.", e);
        }
    }

    @Override
    public void sendServiceRequest(String requesterEmail, String objective, String adminEmail) {
        String html = ServiceRequestEmailTemplate.buildHtml(requesterEmail, objective);
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", adminEmail)),
                "subject", SUBJECT_SERVICE_REQUEST,
                "htmlContent", html
        );
        try {
            restClient.post()
                    .uri(BREVO_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Demande de service envoyée pour {} à {}", requesterEmail, adminEmail);
        } catch (Exception e) {
            log.error("Échec envoi demande de service pour {} à {}: {}", requesterEmail, adminEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer la demande de service.", e);
        }
    }

    @Override
    public void sendTemporaryPassword(String toEmail, String temporaryPassword, String loginUrl, boolean isOwner) {
        String html = TemporaryPasswordEmailTemplate.buildHtml(temporaryPassword, loginUrl, isOwner);
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", toEmail)),
                "subject", SUBJECT_TEMPORARY_PASSWORD,
                "htmlContent", html
        );
        try {
            restClient.post()
                    .uri(BREVO_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Mot de passe temporaire envoyé à {}", toEmail);
        } catch (Exception e) {
            log.error("Échec envoi mot de passe temporaire Brevo vers {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email avec le mot de passe temporaire.", e);
        }
    }
}
