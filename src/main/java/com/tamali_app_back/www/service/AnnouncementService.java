package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.AnnouncementDto;
import com.tamali_app_back.www.entity.GlobalAnnouncement;
import com.tamali_app_back.www.repository.GlobalAnnouncementRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final GlobalAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public Optional<AnnouncementDto> getCurrent() {
        return announcementRepository.findTopByActiveTrueOrderByCreatedAtDesc()
                .map(a -> new AnnouncementDto(a.getId(), a.getMessage()));
    }

    @Transactional
    public AnnouncementDto setCurrent(String message) {
        announcementRepository.findTopByActiveTrueOrderByCreatedAtDesc()
                .ifPresent(a -> {
                    a.setActive(false);
                    announcementRepository.save(a);
                });
        GlobalAnnouncement created = announcementRepository.save(
                GlobalAnnouncement.builder().message(message.trim()).active(true).build());
        return new AnnouncementDto(created.getId(), created.getMessage());
    }

    @Transactional
    public void clearCurrent() {
        announcementRepository.findTopByActiveTrueOrderByCreatedAtDesc()
                .ifPresent(a -> {
                    a.setActive(false);
                    announcementRepository.save(a);
                });
    }

    @Transactional(readOnly = true)
    public void broadcastEmail(String subject, String message) {
        List<String> emails = userRepository.findAllEmailsByEnabledTrue();
        if (emails.isEmpty()) {
            log.warn("Aucun utilisateur actif pour le broadcast email.");
            return;
        }
        String htmlBody = BroadcastEmailTemplate.buildHtml(message);
        for (String email : emails) {
            try {
                mailService.sendBroadcastEmail(email, subject, htmlBody);
            } catch (Exception e) {
                log.error("Échec envoi broadcast à {}: {}", email, e.getMessage());
            }
        }
        log.info("Broadcast email envoyé à {} destinataire(s).", emails.size());
    }
}
