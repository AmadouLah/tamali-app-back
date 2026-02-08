package com.tamali_app_back.www.enums;

/**
 * Rôles de l'application (comptabilité, gestion de stock, ventes – Mali).
 * ADMIN : créateur / super admin de l'app.
 * OWNER : propriétaire de la boutique (compte créé par le dev).
 * MANAGER : gestion produits, stock, rapports.
 * CASHIER : caissier, ventes et reçus.
 */
public enum RoleType {
    ADMIN,
    OWNER,
    MANAGER,
    CASHIER
}
