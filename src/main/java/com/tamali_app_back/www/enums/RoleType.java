package com.tamali_app_back.www.enums;

/**
 * Rôles globaux de l'application Tamali.
 * SUPER_ADMIN : propriétaire global du site (créateur de l'application).
 * BUSINESS_OWNER : propriétaire d'une entreprise qui crée son business sur la plateforme.
 * BUSINESS_ASSOCIATE : associé d'un propriétaire d'entreprise, peut uniquement ajouter des ventes.
 */
public enum RoleType {
    SUPER_ADMIN,
    BUSINESS_OWNER,
    BUSINESS_ASSOCIATE
}
