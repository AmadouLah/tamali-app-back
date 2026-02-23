package com.tamali_app_back.www.service;

/**
 * Template HTML pour l'email de communication envoyé à tous les utilisateurs (Super Admin).
 */
public final class BroadcastEmailTemplate {

    private BroadcastEmailTemplate() {}

    /**
     * Construit le HTML de l'email à partir du message (texte converti en paragraphes HTML).
     */
    public static String buildHtml(String message) {
        String bodyContent = message != null && !message.isBlank()
                ? "<p style=\"margin:0 0 16px; font-size:15px; color:#475569; line-height:1.6;\">"
                + escapeHtml(message).replace("\n", "</p><p style=\"margin:0 0 16px; font-size:15px; color:#475569; line-height:1.6;\">")
                + "</p>"
                : "<p style=\"margin:0; font-size:15px; color:#475569;\">—</p>";
        return buildEmailBody(bodyContent);
    }

    private static String buildEmailBody(String bodyContent) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Communication Tamali</title>
            </head>
            <body style="margin:0; padding:0; font-family: system-ui, -apple-system, sans-serif; background:#f5f5f5;">
              <table width="100%" cellpadding="0" cellspacing="0" style="max-width:600px; margin:32px auto; background:#fff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                <tr>
                  <td style="padding:40px 32px;">
                    <p style="margin:0 0 8px; font-size:14px; color:#64748b;">Communication plateforme</p>
                    <p style="margin:0 0 24px; font-size:28px; font-weight:600; color:#0f172a;">Tamali</p>
                    <div style="margin:0 0 24px;">
                      BODY_PLACEHOLDER
                    </div>
                    <p style="margin:24px 0 0; font-size:13px; color:#94a3b8;">Cet email a été envoyé à tous les utilisateurs de la plateforme.</p>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.replace("BODY_PLACEHOLDER", bodyContent);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
