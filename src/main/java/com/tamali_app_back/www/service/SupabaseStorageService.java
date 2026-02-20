package com.tamali_app_back.www.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final RestTemplate restTemplate = new RestTemplate();
    private final String supabaseUrl;
    private final String bucket;
    private final String serviceRoleKey;

    public SupabaseStorageService(
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.bucket:receipt}") String bucket,
            @Value("${app.storage.supabase.service-role-key:}") String serviceRoleKey) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.replaceAll("/$", "") : "";
        this.bucket = bucket;
        this.serviceRoleKey = serviceRoleKey;
    }

    /**
     * Upload un fichier vers Supabase Storage et retourne l'URL publique.
     */
    public String upload(String path, MultipartFile file) throws IOException {
        if (supabaseUrl.isBlank() || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase storage non configuré.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 5 Mo).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !java.util.Arrays.asList(ALLOWED_TYPES).contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé (images uniquement).");
        }

        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return Objects.requireNonNullElse(file.getOriginalFilename(), "logo");
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Échec upload Supabase: " + response.getBody());
        }
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    public String uploadLogo(UUID businessId, MultipartFile file) throws IOException {
        String ext = extFromContentType(file.getContentType());
        String path = "logos/" + businessId + "/" + UUID.randomUUID() + ext;
        return upload(path, file);
    }

    /**
     * Upload un PDF (reçu) vers Supabase Storage et retourne l'URL publique.
     * Utilise upsert pour écraser si le reçu existe déjà (ex: régénération).
     */
    public String uploadReceiptPdf(UUID businessId, UUID saleId, byte[] pdfBytes) {
        if (supabaseUrl.isBlank() || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase storage non configuré.");
        }
        String path = "receipts/" + businessId + "/" + saleId + ".pdf";
        return uploadBytes(path, pdfBytes, "application/pdf", true);
    }

    /**
     * Upload de contenu binaire vers Supabase Storage.
     */
    public String uploadBytes(String path, byte[] content, String contentType) {
        return uploadBytes(path, content, contentType, false);
    }

    /**
     * Upload de contenu binaire vers Supabase Storage.
     * @param upsert si true, écrase le fichier existant (évite 409 Duplicate)
     */
    public String uploadBytes(String path, byte[] content, String contentType, boolean upsert) {
        if (supabaseUrl.isBlank() || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase storage non configuré.");
        }
        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceRoleKey);
        headers.set("apikey", serviceRoleKey);
        if (upsert) {
            headers.set("x-upsert", "true");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        String filename = path.substring(path.lastIndexOf('/') + 1);
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Échec upload Supabase: " + response.getBody());
        }
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    private static String extFromContentType(String ct) {
        if (ct == null) return ".png";
        return switch (ct) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }
}
