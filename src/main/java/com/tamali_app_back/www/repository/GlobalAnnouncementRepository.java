package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.GlobalAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GlobalAnnouncementRepository extends JpaRepository<GlobalAnnouncement, UUID> {

    Optional<GlobalAnnouncement> findTopByActiveTrueOrderByCreatedAtDesc();
}
