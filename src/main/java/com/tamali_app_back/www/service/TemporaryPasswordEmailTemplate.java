package com.tamali_app_back.www.service;

/**
 * Template HTML pour l'email de mot de passe temporaire.
 */
public final class TemporaryPasswordEmailTemplate {

    private static final String SENDER_NAME = "Tamali";

    private TemporaryPasswordEmailTemplate() {}

    /**
     * Construit le HTML de l'email avec le mot de passe temporaire et le lien de connexion.
     */
    public static String buildHtml(String temporaryPassword, String loginUrl) {
        String escapedPassword = escapeHtml(temporaryPassword != null ? temporaryPassword : "");
        String escapedUrl = escapeHtml(loginUrl != null ? loginUrl : "");
        
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Vos identifiants Tamali</title>
            </head>
            <body style="margin:0; padding:0; font-family: system-ui, -apple-system, sans-serif; background:#f5f5f5;">
              <table width="100%" cellpadding="0" cellspacing="0" style="max-width:500px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                <tr>
                  <td style="padding:40px 32px;">
                    <p style="margin:0 0 8px; font-size:14px; color:#64748b; text-align:center;">Bienvenue sur</p>
                    <p style="margin:0 0 32px; font-size:28px; font-weight:600; color:#0f172a; text-align:center;">Tamali</p>
                    <p style="margin:0 0 24px; font-size:15px; color:#475569; line-height:1.6;">
                      Votre compte propriétaire d'entreprise a été créé. Voici vos identifiants temporaires :
                    </p>
                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px; padding:20px; margin:24px 0;">
                      <p style="margin:0 0 8px; font-size:13px; color:#64748b; font-weight:500;">MOT DE PASSE TEMPORAIRE</p>
                      <p style="margin:0; font-size:24px; font-weight:700; letter-spacing:2px; color:#0f172a; font-family:monospace;">PASSWORD_PLACEHOLDER</p>
                    </div>
                    <p style="margin:24px 0; font-size:14px; color:#dc2626; font-weight:500;">
                      ⚠️ Important : Vous devez changer ce mot de passe lors de votre première connexion.
                    </p>
                    <p style="margin:0 0 32px; font-size:15px; color:#475569; line-height:1.6;">
                      Cliquez sur le bouton ci-dessous pour accéder à la page de connexion :
                    </p>
                    <div style="text-align:center;">
                      <a href="LOGIN_URL_PLACEHOLDER" style="display:inline-block; padding:14px 32px; background:#0f172a; color:#fff; text-decoration:none; border-radius:8px; font-weight:500; font-size:15px;">
                        Se connecter
                      </a>
                    </div>
                    <p style="margin:32px 0 0; font-size:13px; color:#94a3b8; text-align:center;">
                      Si vous n'avez pas demandé ce compte, ignorez cet email.
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
        
        return template.replace("PASSWORD_PLACEHOLDER", escapedPassword)
                      .replace("LOGIN_URL_PLACEHOLDER", escapedUrl);
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
