package com.tamali_app_back.www.service;

/**
 * Template HTML pour l'email de demande d'utilisation du service envoyé au SUPER_ADMIN.
 */
public final class ServiceRequestEmailTemplate {

    private ServiceRequestEmailTemplate() {}

    /**
     * Construit le HTML de l'email de demande avec les informations du demandeur.
     */
    public static String buildHtml(String requesterEmail, String objective) {
        String escapedEmail = escapeHtml(requesterEmail);
        String escapedObjective = escapeHtml(objective);
        
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Nouvelle demande d'utilisation — Tamali</title>
            </head>
            <body style="margin:0; padding:0; font-family: system-ui, -apple-system, sans-serif; background:#f5f5f5;">
              <table width="100%" cellpadding="0" cellspacing="0" style="max-width:600px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                <tr>
                  <td style="padding:40px 32px;">
                    <p style="margin:0 0 8px; font-size:14px; color:#64748b;">Nouvelle demande d'utilisation</p>
                    <p style="margin:0 0 24px; font-size:28px; font-weight:600; color:#0f172a;">Tamali</p>
                    <p style="margin:0 0 20px; font-size:15px; color:#475569; line-height:1.6;">
                      Une nouvelle demande d'utilisation du service Tamali a été reçue.
                    </p>
                    <div style="background:#f8fafc; border-left:4px solid #0f172a; padding:16px 20px; margin:24px 0; border-radius:4px;">
                      <p style="margin:0 0 12px; font-size:14px; font-weight:600; color:#0f172a;">Email du demandeur :</p>
                      <p style="margin:0 0 20px; font-size:15px; color:#475569;">EMAIL_PLACEHOLDER</p>
                      <p style="margin:0 0 12px; font-size:14px; font-weight:600; color:#0f172a;">Objectif :</p>
                      <p style="margin:0; font-size:15px; color:#475569; line-height:1.6;">OBJECTIVE_PLACEHOLDER</p>
                    </div>
                    <p style="margin:24px 0 0; font-size:13px; color:#94a3b8;">
                      Connectez-vous à l'administration pour traiter cette demande.
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
        
        return template.replace("EMAIL_PLACEHOLDER", escapedEmail)
                      .replace("OBJECTIVE_PLACEHOLDER", escapedObjective);
    }
    
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
