package com.tamali_app_back.www.service;

/**
 * Envoi d'emails (code de vérification, etc.).
 */
public interface MailService {

    /**
     * Envoie l'email contenant le code de vérification à 6 chiffres.
     * @param toEmail destinataire
     * @param code code à 6 chiffres
     * @param validityMinutes durée de validité du code (affichée dans l'email)
     */
    void sendVerificationCode(String toEmail, String code, int validityMinutes);
}
