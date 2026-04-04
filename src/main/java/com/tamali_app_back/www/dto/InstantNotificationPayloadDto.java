package com.tamali_app_back.www.dto;

import java.time.Instant;

/**
 * Événement SSE : même identifiant que le Web Push pour unifier la notification système (tag).
 */
public record InstantNotificationPayloadDto(String message, Instant sentAt, String notificationId) {}
