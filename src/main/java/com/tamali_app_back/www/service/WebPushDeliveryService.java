package com.tamali_app_back.www.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tamali_app_back.www.config.WebPushProperties;
import com.tamali_app_back.www.dto.request.InstantNotificationRequest;
import com.tamali_app_back.www.entity.UserPushSubscription;
import com.tamali_app_back.www.repository.UserPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushAsyncService;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushDeliveryService {

    /** Instance locale : Spring Boot 4 n’expose pas toujours un bean {@link ObjectMapper}. */
    private static final ObjectMapper PUSH_JSON = new ObjectMapper();

    private final WebPushProperties webPushProperties;
    private final UserPushSubscriptionRepository subscriptionRepository;

    @Value("${app.frontend-url:https://tamali.vercel.app/}")
    private String frontendUrl;

    private volatile PushAsyncService pushAsyncService;

    public WebPushSendStats deliver(InstantNotificationRequest request, String message, String notificationId) {
        if (!webPushProperties.isConfigured()) {
            log.debug("Web Push : clés VAPID absentes, envoi ignoré.");
            return new WebPushSendStats(0, 0);
        }
        PushAsyncService service;
        try {
            service = getOrCreatePushAsyncService();
        } catch (GeneralSecurityException e) {
            log.warn("Web Push : initialisation PushAsyncService impossible.", e);
            return new WebPushSendStats(0, 0);
        }
        if (service == null) {
            return new WebPushSendStats(0, 0);
        }

        List<UserPushSubscription> targets = resolveTargets(request);
        if (targets.isEmpty()) {
            return new WebPushSendStats(0, 0);
        }

        byte[] payload;
        try {
            payload = buildAngularPushPayload(message, notificationId);
        } catch (Exception e) {
            log.warn("Web Push : sérialisation du corps impossible.", e);
            return new WebPushSendStats(targets.size(), 0);
        }

        int delivered = 0;
        for (UserPushSubscription sub : targets) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuthSecret(),
                        payload);
                Response res = service.send(notification).get(45, TimeUnit.SECONDS);
                int code = res.getStatusCode();
                if (code == 410 || code == 404) {
                    subscriptionRepository.delete(sub);
                } else if (code >= 200 && code < 300) {
                    delivered++;
                } else {
                    log.debug("Web Push : code HTTP {} pour endpoint …{}", code, shortened(sub.getEndpoint()));
                }
            } catch (Exception e) {
                log.debug("Web Push : échec pour …{} — {}", shortened(sub.getEndpoint()), e.getMessage());
            }
        }
        return new WebPushSendStats(targets.size(), delivered);
    }

    private PushAsyncService getOrCreatePushAsyncService() throws GeneralSecurityException {
        if (pushAsyncService == null) {
            synchronized (this) {
                if (pushAsyncService == null) {
                    pushAsyncService = new PushAsyncService(
                            webPushProperties.vapidPublicKey().trim(),
                            webPushProperties.vapidPrivateKey().trim(),
                            webPushProperties.subject().trim());
                }
            }
        }
        return pushAsyncService;
    }

    private List<UserPushSubscription> resolveTargets(InstantNotificationRequest request) {
        return switch (request.scope()) {
            case ALL -> subscriptionRepository.findAll();
            case ROLE -> subscriptionRepository.findAllByUserHavingRole(request.roleType());
            case USERS -> subscriptionRepository.findByUser_IdIn(request.userIds());
        };
    }

    private byte[] buildAngularPushPayload(String message, String notificationId) throws Exception {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String icon = base + "/favicon.ico";
        String tag = "tamali-instant-" + notificationId;

        ObjectNode notification = PUSH_JSON.createObjectNode();
        notification.put("title", "Tamali");
        notification.put("body", message);
        notification.put("icon", icon);
        notification.put("tag", tag);
        notification.put("requireInteraction", true);
        notification.put("silent", false);

        ObjectNode root = PUSH_JSON.createObjectNode();
        root.set("notification", notification);
        return PUSH_JSON.writeValueAsBytes(root);
    }

    private static String shortened(String endpoint) {
        if (endpoint == null)
            return "";
        return endpoint.length() > 48 ? endpoint.substring(endpoint.length() - 48) : endpoint;
    }

    public record WebPushSendStats(int targets, int delivered) {
    }
}
