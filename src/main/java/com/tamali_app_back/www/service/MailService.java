package com.tamali_app_back.www.service;

/**
 * Envoi d'emails (code de vérification, invitations, etc.).
 */
public interface MailService {

    /**
     * Envoie l'email contenant le code de vérification à 6 chiffres.
     * @param toEmail destinataire
     * @param code code à 6 chiffres
     * @param validityMinutes durée de validité du code (affichée dans l'email)
     */
    void sendVerificationCode(String toEmail, String code, int validityMinutes);

    /**
     * Envoie l'email d'invitation pour créer un compte BUSINESS_OWNER.
     * @param toEmail destinataire
     * @param invitationLink lien d'invitation vers la plateforme
     * @param validityDays durée de validité du lien (affichée dans l'email)
     */
    void sendInvitation(String toEmail, String invitationLink, int validityDays);
}
