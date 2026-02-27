package com.fincen.sar.core.enums;

/**
 * FinCEN BSA XML 2.0 ActivityPartyTypeCode values.
 * Each SAR activity requires specific party types in the batch XML.
 */
public enum PartyTypeCode {
    TRANSMITTER(8, "Transmitter"),
    TRANSMITTER_CONTACT(9, "Transmitter Contact"),
    FILING_INSTITUTION(35, "Filing Institution"),
    DESIGNATED_CONTACT_OFFICE(46, "Designated Contact Office"),
    LAW_ENFORCEMENT_AGENCY(41, "Law Enforcement Agency"),
    LAW_ENFORCEMENT_CONTACT(42, "Law Enforcement Contact"),
    FI_WHERE_ACTIVITY_OCCURRED(33, "Financial Institution Where Activity Occurred"),
    BRANCH_WHERE_ACTIVITY_OCCURRED(34, "Branch Where Activity Occurred"),
    SUBJECT(23, "Subject");

    private final int code;
    private final String description;

    PartyTypeCode(int code, String description) {
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
