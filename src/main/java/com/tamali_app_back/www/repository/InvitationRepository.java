package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    Optional<Invitation> findByEmailAndUsedFalse(String email);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
