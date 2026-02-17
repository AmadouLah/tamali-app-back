package com.tamali_app_back.www.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Réponse d'erreur unique, lisible par l'utilisateur.
 * message : texte court à afficher.
 * code : identifiant technique optionnel (ex. VALIDATION_ERROR).
 * errors : détails par champ (ex. validation), absent si vide.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String message,
        String code,
        List<String> errors
) {
    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse(message, code, null);
    }

    public static ErrorResponse of(String message, String code, List<String> errors) {
        return new ErrorResponse(message, code, errors != null && !errors.isEmpty() ? errors : null);
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null, null);
    }
}
