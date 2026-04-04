package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.config.WebPushProperties;
import com.tamali_app_back.www.dto.VapidPublicKeyDto;
import com.tamali_app_back.www.dto.request.PushSubscriptionSubscribeRequest;
import com.tamali_app_back.www.dto.request.PushSubscriptionUnregisterRequest;
import com.tamali_app_back.www.service.WebPushSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/push")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushProperties webPushProperties;
    private final WebPushSubscriptionService webPushSubscriptionService;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<VapidPublicKeyDto> vapidPublicKey() {
        if (!webPushProperties.isConfigured()) {
            return ResponseEntity.ok(new VapidPublicKeyDto(""));
        }
        return ResponseEntity.ok(new VapidPublicKeyDto(webPushProperties.vapidPublicKey().trim()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PushSubscriptionSubscribeRequest body
    ) {
        webPushSubscriptionService.register(userId, body);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PushSubscriptionUnregisterRequest body
    ) {
        webPushSubscriptionService.unregister(userId, body.endpoint());
        return ResponseEntity.noContent().build();
    }
}
