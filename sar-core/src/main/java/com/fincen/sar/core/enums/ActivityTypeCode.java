package com.fincen.sar.core.enums;

/**
 * FinCEN SAR Suspicious Activity subtype codes per BSA XML 2.0 schema.
 * References: FinCENReferenceCodes.xsd SuspiciousActivitySubtypeID values.
 */
public enum ActivityTypeCode {

    // Fraud
    BRIBERY_GRATUITY(1, "Bribery/gratuity"),
    CHECK_FRAUD(2, "Check fraud"),
    CHECK_KITING(3, "Check kiting"),
    COMMERCIAL_LOAN_FRAUD(4, "Commercial loan fraud"),
    CONSUMER_LOAN_FRAUD(5, "Consumer loan fraud"),
    COUNTERFEIT_CHECK(6, "Counterfeit check"),
    COUNTERFEIT_CREDIT_DEBIT_CARD(7, "Counterfeit credit/debit card"),
    COUNTERFEIT_INSTRUMENT_OTHER(8, "Counterfeit instrument (other)"),
    CREDIT_CARD_FRAUD(9, "Credit card fraud"),
    DEBIT_CARD_FRAUD(10, "Debit card fraud"),
    DEFALCATION_EMBEZZLEMENT(11, "Defalcation/embezzlement"),
    FALSE_STATEMENT(12, "False statement"),
    MISUSE_OF_POSITION(13, "Misuse of position or self-dealing"),
    MORTGAGE_LOAN_FRAUD(14, "Mortgage loan fraud"),
    MYSTERIOUS_DISAPPEARANCE(15, "Mysterious disappearance"),
    WIRE_TRANSFER_FRAUD(16, "Wire transfer fraud"),
    FRAUD_OTHER(17, "Other (Fraud)"),

    // Money Laundering
    IDENTIFICATION_DOCUMENTATION(18, "Identification documentation"),
    MONEY_LAUNDERING(19, "Money laundering"),
    MONEY_LAUNDERING_OTHER(20, "Other (Money laundering)"),

    // Terrorist Financing
    TERRORIST_FINANCING(21, "Terrorist financing"),
    TERRORIST_FINANCING_OTHER(22, "Other (Terrorist financing)"),

    // Structuring
    GAMING_ACTIVITIES(23, "Gaming activities"),
    STRUCTURING(24, "Structuring"),
    TRANSACTIONS_BELOW_THRESHOLD(25, "Transaction(s) below BSA threshold"),
    TWO_OR_MORE_INDIVIDUALS(26, "Two or more individuals working together"),
    UNUSUAL_MULTIPLE_TRANSACTION_TYPES(27, "Unusual use of multiple transaction types"),
    STRUCTURING_ML_OTHER(28, "Other (Structuring/Money laundering)"),

    // Cyber Events / Identity
    ACCOUNT_TAKEOVER(29, "Account takeover"),
    COMPUTER_INTRUSION(30, "Computer intrusion"),
    CREDIT_DEBIT_CARD_THEFT(31, "Credit card/debit card theft"),
    DEBIT_CARD_FRAUD_OTHER(32, "Debit card fraud (other)"),
    ELDER_FINANCIAL_EXPLOITATION(33, "Elder financial exploitation"),
    EMAIL_COMPROMISE(34, "E-mail compromise/E-mail related fraud"),
    EXTORTION_BLACKMAIL(35, "Extortion/blackmail"),
    FALSE_POLICE_REPORT(36, "False police report"),
    HOME_EQUITY_FRAUD(37, "Home equity loan/line fraud"),
    HUMAN_TRAFFICKING(38, "Human trafficking"),
    IDENTITY_THEFT(39, "Identity theft"),
    ILLICIT_MARKETPLACE(40, "Illicit marketplace"),
    INSURANCE_FRAUD(41, "Insurance fraud"),
    INVESTMENT_FRAUD(42, "Investment fraud"),
    LOTTERY_SWEEPSTAKES_SCAM(43, "Lottery/sweepstakes scams"),
    MALWARE_RANSOMWARE(44, "Malware/ransomware"),
    MASS_MARKETING_FRAUD(45, "Mass marketing fraud"),
    PONZI_PYRAMID_SCHEME(46, "Ponzi scheme/pyramid scheme"),
    ROMANCE_FRAUD(47, "Romance fraud"),
    SECURITIES_FRAUD(48, "Securities fraud"),
    SOCIAL_ENGINEERING(49, "Social engineering"),
    TAX_REFUND_FRAUD(50, "Tax refund fraud"),
    TELEMARKETING_PHONE_FRAUD(51, "Telemarketing/phone fraud"),
    TRADE_BASED_ML(52, "Trade-based money laundering/Black market peso exchange");

    private final int code;
    private final String description;

    ActivityTypeCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ActivityTypeCode fromCode(int code) {
        for (ActivityTypeCode type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown ActivityTypeCode: " + code);
    }
}
