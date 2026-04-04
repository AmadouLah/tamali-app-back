package com.tamali_app_back.www.dto;

import java.time.Instant;

/**
 * Événement SSE sérialisé vers le client (toast + notification système optionnelle).
 */
public record InstantNotificationPayloadDto(String message, Instant sentAt) {}
