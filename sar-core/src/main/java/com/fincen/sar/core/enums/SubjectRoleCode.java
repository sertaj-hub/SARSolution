package com.fincen.sar.core.enums;

/**
 * Subject relationship to the suspicious activity per FinCEN SAR Part I.
 */
public enum SubjectRoleCode {
    ACCOUNTANT("A", "Accountant"),
    AGENT("AG", "Agent"),
    APPRAISER("AP", "Appraiser"),
    ATTORNEY("AT", "Attorney"),
    BORROWER("BO", "Borrower"),
    BROKER_DEALER("BD", "Broker-dealer"),
    CUSTOMER("CU", "Customer"),
    DIRECTOR("DI", "Director"),
    EMPLOYEE("EM", "Employee"),
    LOAN_APPLICANT("LA", "Loan applicant"),
    OFFICER("OF", "Officer"),
    OTHER("OT", "Other"),
    OWNER("OW", "Owner"),
    SHAREHOLDER("SH", "Shareholder");

    private final String code;
    private final String description;

    SubjectRoleCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
