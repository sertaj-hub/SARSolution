package com.fincen.sar.core.enums;

/**
 * FinCEN BSA E-Filing batch submission statuses.
 */
public enum FilingStatus {
    PENDING("Pending batch processing"),
    BATCH_GENERATED("XML batch file generated"),
    SUBMITTED("Submitted to FinCEN BSA E-Filing"),
    PROCESSING("Being processed by FinCEN"),
    ACCEPTED("Accepted by FinCEN - BSA ID assigned"),
    REJECTED("Rejected by FinCEN with error codes"),
    PARTIALLY_ACCEPTED("Partially accepted with warnings");

    private final String description;

    FilingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
