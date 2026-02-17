package com.tamali_app_back.www.service;

/**
 * Template HTML unique pour l'email de code de vérification (6 chiffres).
 * Style épuré et lisible.
 */
public final class VerificationEmailTemplate {

    private static final String SENDER_NAME = "Tamali";

    private VerificationEmailTemplate() {}

    /**
     * Construit le HTML de l'email avec le code à 6 chiffres.
     */
    public static String buildHtml(String code, int validityMinutes) {
        String escapedCode = escapeHtml(code != null ? code : "");
        
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Code de connexion</title>
            </head>
            <body style="margin:0; padding:0; font-family: system-ui, -apple-system, sans-serif; background:#f5f5f5;">
              <table width="100%" cellpadding="0" cellspacing="0" style="max-width:420px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                <tr>
                  <td style="padding:40px 32px; text-align:center;">
                    <p style="margin:0 0 8px; font-size:14px; color:#64748b;">Votre code de connexion</p>
                    <p style="margin:0 0 24px; font-size:24px; font-weight:600; color:#0f172a;">Tamali</p>
                    <p style="margin:0 0 20px; font-size:32px; font-weight:700; letter-spacing:8px; color:#0f172a;">CODE_PLACEHOLDER</p>
                    <p style="margin:0; font-size:13px; color:#94a3b8;">Valide MINUTES_PLACEHOLDER minutes. Ne partagez ce code avec personne.</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
        
        return template.replace("CODE_PLACEHOLDER", escapedCode)
                      .replace("MINUTES_PLACEHOLDER", String.valueOf(validityMinutes));
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
