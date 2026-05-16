package com.fincen.sar.core.enums;

/**
 * FinCEN EFL_SARXBatchSchema.xsd ValidateActivityPartyCodeType values.
 */
public enum PartyTypeCode {
    TRANSMITTER(35, "Transmitter"),
    TRANSMITTER_CONTACT(37, "Transmitter Contact"),
    FILING_INSTITUTION(30, "Reporting Financial Institution"),
    CONTACT_FOR_ASSISTANCE(8, "Contact for Assistance"),
    FI_WHERE_ACTIVITY_OCCURRED(34, "Transaction Location Business"),
    BRANCH_WHERE_ACTIVITY_OCCURRED(46, "Transaction Location Branch"),
    SUBJECT(33, "Subject");

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
