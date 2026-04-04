package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.request.PushSubscriptionSubscribeRequest;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.entity.UserPushSubscription;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.repository.UserPushSubscriptionRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebPushSubscriptionService {

    private final UserPushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(UUID userId, PushSubscriptionSubscribeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable."));
        String p256dh = request.keys().p256dh();
        String auth = request.keys().auth();
        String endpoint = request.endpoint();

        subscriptionRepository.findByEndpoint(endpoint).ifPresentOrElse(
                existing -> updateExisting(existing, user, p256dh, auth),
                () -> subscriptionRepository.save(UserPushSubscription.builder()
                        .user(user)
                        .endpoint(endpoint)
                        .p256dh(p256dh)
                        .authSecret(auth)
                        .build())
        );
    }

    private void updateExisting(UserPushSubscription existing, User user, String p256dh, String auth) {
        existing.setUser(user);
        existing.setP256dh(p256dh);
        existing.setAuthSecret(auth);
        subscriptionRepository.save(existing);
    }

    @Transactional
    public void unregister(UUID userId, String endpoint) {
        subscriptionRepository.deleteByEndpointAndUserId(endpoint, userId);
    }
}
