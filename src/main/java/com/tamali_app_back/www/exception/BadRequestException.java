package com.tamali_app_back.www.exception;

/**
 * À lancer pour une requête invalide (données incohérentes, règle métier non respectée).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
