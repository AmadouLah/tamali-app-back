package com.tamali_app_back.www.exception;

/**
 * À lancer quand une ressource demandée n'existe pas (ex. GET /api/businesses/{id}).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " introuvable : " + id);
    }
}
