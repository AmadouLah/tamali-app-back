package com.tamali_app_back.www.service;

/**
 * Template HTML pour l'email envoyé lors d'une réinitialisation de mot de passe par le SUPER_ADMIN.
 * Doit être différent de l'email de création de compte.
 */
public final class PasswordResetTemporaryPasswordEmailTemplate {

    private static final String INTRO = "Votre mot de passe a été réinitialisé suite à votre demande. Voici un mot de passe temporaire pour vous reconnecter :";

    private PasswordResetTemporaryPasswordEmailTemplate() {}

    public static String buildHtml(String temporaryPassword, String loginUrl) {
        String escapedPassword = escapeHtml(temporaryPassword != null ? temporaryPassword : "");
        String escapedUrl = escapeHtml(loginUrl != null ? loginUrl : "");
        String intro = escapeHtml(INTRO);
        return buildEmailBody(escapedPassword, escapedUrl, intro);
    }

    private static String buildEmailBody(String escapedPassword, String escapedUrl, String intro) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Réinitialisation de votre mot de passe — Tamali</title>
            </head>
            <body style="margin:0; padding:0; font-family: system-ui, -apple-system, sans-serif; background:#f5f5f5;">
              <table width="100%" cellpadding="0" cellspacing="0" style="max-width:520px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                <tr>
                  <td style="padding:40px 32px;">
                    <p style="margin:0 0 8px; font-size:14px; color:#64748b; text-align:center;">Sécurité</p>
                    <p style="margin:0 0 28px; font-size:28px; font-weight:700; color:#0f172a; text-align:center;">Tamali</p>

                    <p style="margin:0 0 16px; font-size:15px; color:#475569; line-height:1.6;">
                      INTRO_PLACEHOLDER
                    </p>

                    <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px; padding:18px; margin:20px 0;">
                      <p style="margin:0 0 8px; font-size:12px; color:#64748b; font-weight:600; letter-spacing:0.6px;">MOT DE PASSE TEMPORAIRE</p>
                      <p style="margin:0; font-size:24px; font-weight:800; letter-spacing:2px; color:#0f172a; font-family:monospace;">PASSWORD_PLACEHOLDER</p>
                    </div>

                    <div style="background:#fff7ed; border:1px solid #fdba74; border-radius:10px; padding:14px 16px; margin:18px 0;">
                      <p style="margin:0; font-size:13px; color:#9a3412; line-height:1.5;">
                        ⚠️ Important : une fois connecté(e), changez immédiatement ce mot de passe depuis vos paramètres.
                      </p>
                    </div>

                    <p style="margin:0 0 16px; font-size:15px; color:#475569; line-height:1.6;">
                      Cliquez sur le bouton ci-dessous pour accéder à la page de connexion :
                    </p>

                    <div style="text-align:center;">
                      <a href="LOGIN_URL_PLACEHOLDER" style="display:inline-block; padding:14px 30px; background:#7c3aed; color:#fff; text-decoration:none; border-radius:10px; font-weight:700; font-size:14px;">
                        Se connecter
                      </a>
                    </div>

                    <p style="margin:26px 0 0; font-size:12px; color:#94a3b8; text-align:center;">
                      Si vous n'êtes pas à l'origine de cette demande, veuillez contacter le support.
                    </p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

        return template.replace("INTRO_PLACEHOLDER", intro)
                .replace("PASSWORD_PLACEHOLDER", escapedPassword)
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

