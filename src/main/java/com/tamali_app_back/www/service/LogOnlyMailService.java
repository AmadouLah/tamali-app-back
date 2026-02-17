package com.tamali_app_back.www.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "logonly", matchIfMissing = true)
@Slf4j
public class LogOnlyMailService implements MailService {

    @Override
    public void sendVerificationCode(String toEmail, String code, int validityMinutes) {
        log.info("[MAIL] Code de vérification pour {} : {} (valide {} min)", toEmail, code, validityMinutes);
    }
}
