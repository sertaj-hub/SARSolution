package com.fincen.sar.core.enums;

/**
 * SAR Report lifecycle status aligned with FinCEN BSA E-Filing workflow.
 */
public enum SarStatus {
    DRAFT("Draft - under preparation"),
    IN_REVIEW("In Review - pending compliance approval"),
    APPROVED("Approved - ready for eFiling"),
    SUBMITTED("Submitted - batch sent to FinCEN BSA E-Filing"),
    ACKNOWLEDGED("Acknowledged - FinCEN acceptance confirmed"),
    REJECTED("Rejected - FinCEN returned with errors"),
    AMENDED("Amended - correction submitted"),
    CLOSED("Closed");

    private final String description;

    SarStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
