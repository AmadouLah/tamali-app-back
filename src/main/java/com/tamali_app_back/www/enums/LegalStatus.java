package com.tamali_app_back.www.enums;

public enum LegalStatus {
    SARL("SARL - Société à Responsabilité Limitée"),
    SA("SA - Société Anonyme"),
    SAS("SAS - Société par Actions Simplifiée"),
    EURL("EURL - Entreprise Unipersonnelle à Responsabilité Limitée"),
    SN("SN - Société en Nom Collectif"),
    SCS("SCS - Société en Commandite Simple"),
    SCA("SCA - Société en Commandite par Actions"),
    EI("EI - Entreprise Individuelle"),
    AUTO_ENTREPRENEUR("Auto-entrepreneur"),
    ASSOCIATION("Association"),
    AUTRE("Autre");

    private final String label;

    LegalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
