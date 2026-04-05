package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.InstantNotificationPayloadDto;
import com.tamali_app_back.www.dto.InstantNotificationSendResultDto;
import com.tamali_app_back.www.dto.request.InstantNotificationRequest;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hub SSE en mémoire + délégation Web Push pour les appareils abonnés (app fermée possible).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstantNotificationService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final WebPushDeliveryService webPushDeliveryService;

    public SseEmitter subscribe(UUID userId, String primaryRole) {
        if (userId == null) {
            throw new BadRequestException("Connexion flux : identifiant utilisateur requis.");
        }
        SseEmitter emitter = new SseEmitter(0L);
        Subscriber sub = new Subscriber(emitter, userId, primaryRole != null ? primaryRole : "");
        subscribers.add(sub);
        Runnable remove = () -> subscribers.remove(sub);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().comment("stream-open"));
        } catch (IOException e) {
            subscribers.remove(sub);
            throw new BadRequestException("Impossible d'ouvrir le flux de notifications.");
        }
        return emitter;
    }

    public InstantNotificationSendResultDto sendAsSuperAdmin(String callerRole, InstantNotificationRequest request) {
        if (!RoleType.SUPER_ADMIN.name().equals(callerRole)) {
            throw new BadRequestException("Seul le SUPER_ADMIN peut envoyer des notifications instantanées.");
        }
        String message = request.message().trim();
        if (message.isEmpty()) {
            throw new BadRequestException("Le message ne peut pas être vide.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("Message trop long (max " + MAX_MESSAGE_LENGTH + " caractères).");
        }
        validateScope(request);

        String notificationId = UUID.randomUUID().toString();
        InstantNotificationPayloadDto payload = new InstantNotificationPayloadDto(message, Instant.now(), notificationId);
        int sseSent = 0;
        for (Subscriber sub : subscribers) {
            if (!shouldReceive(sub, request)) {
                continue;
            }
            try {
                sub.emitter().send(SseEmitter.event().data(payload, MediaType.APPLICATION_JSON));
                sseSent++;
            } catch (Throwable t) {
                try {
                    sub.emitter().complete();
                } catch (Throwable ignored) {
                    /* connexion déjà fermée */
                }
                subscribers.remove(sub);
                log.debug("Échec envoi SSE (client déconnecté ou flux fermé), abonné retiré: {}", t.getMessage());
            }
        }
        WebPushDeliveryService.WebPushDeliveryResult push = webPushDeliveryService.deliver(request, message, notificationId);
        return new InstantNotificationSendResultDto(sseSent, push.targets(), push.delivered(), push.configured());
    }

    private static void validateScope(InstantNotificationRequest request) {
        switch (request.scope()) {
            case ALL -> { /* ok */ }
            case ROLE -> {
                if (request.roleType() == null) {
                    throw new BadRequestException("Rôle requis pour une diffusion par rôle.");
                }
            }
            case USERS -> {
                List<UUID> ids = request.userIds();
                if (ids == null || ids.isEmpty()) {
                    throw new BadRequestException("Sélectionnez au moins un utilisateur.");
                }
            }
        }
    }

    private static boolean shouldReceive(Subscriber sub, InstantNotificationRequest request) {
        return switch (request.scope()) {
            case ALL -> true;
            case ROLE -> request.roleType().name().equals(sub.primaryRole());
            case USERS -> request.userIds().contains(sub.userId());
        };
    }

    private record Subscriber(SseEmitter emitter, UUID userId, String primaryRole) {}
}
