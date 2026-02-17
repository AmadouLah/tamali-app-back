package com.tamali_app_back.www.util;

import com.tamali_app_back.www.exception.ResourceNotFoundException;

import java.util.Optional;

/**
 * Utilitaire pour unifier les réponses et éviter la redondance dans les contrôleurs.
 */
public final class ResponseUtil {

    private ResponseUtil() {}

    /**
     * Retourne le DTO ou lance ResourceNotFoundException avec un message clair.
     */
    public static <T> T requireFound(T dto, String resourceName, Object id) {
        if (dto == null) {
            throw new ResourceNotFoundException(resourceName, id);
        }
        return dto;
    }

    /**
     * Retourne la valeur de l'Optional ou lance ResourceNotFoundException.
     */
    public static <T> T requireFound(Optional<T> optional, String resourceName, Object id) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(resourceName, id));
    }
}
