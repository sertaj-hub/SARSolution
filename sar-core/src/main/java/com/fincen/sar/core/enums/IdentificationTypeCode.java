package com.fincen.sar.core.enums;

/**
 * FinCEN BSA PartyIdentification type codes per FinCENReferenceCodes.xsd.
 */
public enum IdentificationTypeCode {
    ALIEN_REGISTRATION(1, "Alien registration number"),
    EIN(2, "EIN"),
    DRIVERS_LICENSE(3, "Driver's license/state ID"),
    FOREIGN_ID(4, "Foreign identification"),
    SSN_ITIN(5, "SSN/ITIN"),
    PASSPORT(6, "Passport"),
    OTHER(7, "Other"),
    NATIONAL_ID(8, "National ID number"),
    UNKNOWN(999, "Unknown");

    private final int code;
    private final String description;

    IdentificationTypeCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
