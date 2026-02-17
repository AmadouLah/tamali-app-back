package com.tamali_app_back.www.service;

/**
 * Template HTML pour l'email d'invitation BUSINESS_OWNER.
 * Style épuré et professionnel.
 */
public final class InvitationEmailTemplate {

    private InvitationEmailTemplate() {}

    /**
     * Construit le HTML de l'email d'invitation avec le lien vers la plateforme.
     */
    public static String buildHtml(String invitationLink, int validityDays) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Invitation Tamali</title>
            </head>
            <body style="margin:0; padding:0; font-family: system-ui, -apple-system, sans-serif; background:#f5f5f5;">
              <table width="100%" cellpadding="0" cellspacing="0" style="max-width:500px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                <tr>
                  <td style="padding:40px 32px; text-align:center;">
                    <p style="margin:0 0 8px; font-size:14px; color:#64748b;">Vous avez été invité à rejoindre</p>
                    <p style="margin:0 0 24px; font-size:28px; font-weight:600; color:#0f172a;">Tamali</p>
                    <p style="margin:0 0 32px; font-size:15px; color:#475569; line-height:1.6;">
                      Créez votre compte et gérez votre entreprise en toute simplicité.
                    </p>
                    <a href="%s" style="display:inline-block; padding:14px 32px; background:#0f172a; color:#fff; text-decoration:none; border-radius:8px; font-weight:500; font-size:15px;">
                      Accepter l'invitation
                    </a>
                    <p style="margin:32px 0 0; font-size:13px; color:#94a3b8;">
                      Ce lien est valide %d jours. Si vous n'avez pas demandé cette invitation, ignorez cet email.
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(invitationLink, validityDays);
    }
}
