package com.fincen.sar.core.enums;

/**
 * FinCEN SAR suspicious activity types.
 * subtypeId = ValidateSuspiciousActivitySubtypeID in EFL_SARXBatchSchema.xsd
 * typeId    = ValidateSuspiciousActivityTypeID  in EFL_SARXBatchSchema.xsd
 */
public enum ActivityTypeCode {

    // --- Structuring (typeId=1) ---
    STRUCTURING(114, 1, "Structuring / Transaction below CTR threshold"),
    TRANSACTIONS_BELOW_BSA_THRESHOLD(113, 1, "Transaction(s) below BSA recordkeeping threshold"),
    ALTERS_CANCELS_FOR_CTR(112, 1, "Alters or cancels transaction to avoid CTR requirement"),
    ALTERS_CANCELS_FOR_BSA(111, 1, "Alters or cancels transaction to avoid BSA recordkeeping requirement"),
    SUSPICIOUS_BSA_INQUIRY(106, 1, "Suspicious inquiry regarding BSA/recordkeeping requirements"),

    // --- Fraud (typeId=3) ---
    CHECK_FRAUD(301, 3, "Check fraud"),
    CONSUMER_LOAN_FRAUD(304, 3, "Consumer loan fraud"),
    CREDIT_DEBIT_CARD_FRAUD(305, 3, "Credit/debit card fraud"),
    WIRE_TRANSFER_FRAUD(312, 3, "Wire transfer fraud"),
    BUSINESS_LOAN_FRAUD(321, 3, "Business loan fraud"),
    ADVANCE_FEE_FRAUD(322, 3, "Advance fee fraud"),
    HEALTHCARE_FRAUD(323, 3, "Healthcare/insurance fraud"),
    PONZI_SCHEME(324, 3, "Ponzi/pyramid scheme"),
    SECURITIES_FRAUD(325, 3, "Securities fraud"),
    MAIL_FRAUD(308, 3, "Mail fraud"),
    MASS_MARKETING_FRAUD(309, 3, "Mass-marketing fraud"),
    PYRAMID_SCHEME(310, 3, "Pyramid scheme"),
    ACH_FRAUD(320, 3, "ACH fraud"),

    // --- Identification Documentation (typeId=4) ---
    CHANGES_SPELLING_OF_NAME(401, 4, "Changes spelling or arrangement of name"),
    MULTIPLE_INDIVIDUALS_SAME_IDENTITY(402, 4, "Multiple individuals with same or similar identities"),
    QUESTIONABLE_FALSE_DOCUMENTATION(403, 4, "Provided questionable or false documentation"),
    REFUSED_AVOIDED_DOCUMENTATION(404, 4, "Refused or avoided request for documentation"),
    SINGLE_INDIVIDUAL_MULTIPLE_IDENTITIES(405, 4, "Single individual with multiple identities"),
    QUESTIONABLE_FALSE_IDENTIFICATION(409, 4, "Provided questionable or false identification"),

    // --- Insurance (typeId=5) ---
    EXCESSIVE_INSURANCE(501, 5, "Excessive insurance"),
    EXCESSIVE_CASH_BORROWING_POLICY(502, 5, "Excessive or unusual cash borrowing against policy/annuity"),
    PROCEEDS_SENT_UNRELATED_THIRD_PARTY(504, 5, "Proceeds sent to unrelated third party"),
    SUSPICIOUS_LIFE_SETTLEMENT(505, 5, "Suspicious life settlement sales insurance"),
    SUSPICIOUS_TERMINATION_POLICY(506, 5, "Suspicious termination of policy or contract"),
    UNCLEAR_NO_INSURABLE_INTEREST(507, 5, "Unclear or no insurable interest"),

    // --- Securities/Futures/Options (typeId=6) ---
    INSIDER_TRADING(601, 6, "Insider trading"),
    MISAPPROPRIATION(603, 6, "Misappropriation"),
    UNAUTHORIZED_POOLING(604, 6, "Unauthorized pooling"),
    MARKET_MANIPULATION(608, 6, "Market manipulation"),
    WASH_TRADING(609, 6, "Wash trading"),

    // --- Terrorist Financing (typeId=7) ---
    TERRORIST_FINANCING(701, 7, "Known or suspected terrorist/terrorist organization"),

    // --- Money Laundering (typeId=8) ---
    EXCHANGES_BILLS(801, 8, "Exchanges small bills for large bills or vice versa"),
    SUSPICIOUS_BENEFICIARY_DESIGNATION(804, 8, "Suspicious designation of beneficiaries/assignees/joint owners"),
    SUSPICIOUS_EFT_WIRE(805, 8, "Suspicious EFT/wire transfers"),
    SUSPICIOUS_GOVERNMENT_PAYMENTS(806, 8, "Suspicious receipt of government payments/benefits"),
    SUSPICIOUS_MULTIPLE_ACCOUNTS(807, 8, "Suspicious use of multiple accounts"),
    SUSPICIOUS_NONCASH_INSTRUMENTS(808, 8, "Suspicious use of noncash monetary instruments"),
    SUSPICIOUS_THIRD_PARTY_TRANSACTORS(809, 8, "Suspicious use of third-party transactors (straw-man)"),
    OUT_OF_PATTERN_TRANSACTION(812, 8, "Transaction out of pattern for customer(s)"),
    SUSPICIOUS_PHYSICAL_CONDITION_FUNDS(820, 8, "Suspicious concerning physical condition of funds"),
    SUSPICIOUS_SOURCE_OF_FUNDS(821, 8, "Suspicious concerning source of funds"),
    SUSPICIOUS_EXCHANGE_CURRENCIES(822, 8, "Suspicious exchange of currencies"),
    TRADE_BASED_ML(823, 8, "Trade-based money laundering/Black market peso exchange"),
    FUNNEL_ACCOUNT(824, 8, "Funnel account"),

    // --- Other Suspicious Activities (typeId=9) ---
    BRIBERY_GRATUITY(901, 9, "Bribery or gratuity"),
    EMBEZZLEMENT_THEFT(903, 9, "Embezzlement/theft/disappearance of funds"),
    FORGERIES(904, 9, "Forgeries"),
    IDENTITY_THEFT(905, 9, "Identity theft"),
    CORRUPTION_DOMESTIC(907, 9, "Suspected public/private corruption (domestic)"),
    CORRUPTION_FOREIGN(908, 9, "Suspected public/private corruption (foreign)"),
    INFORMAL_VALUE_TRANSFER(909, 9, "Suspicious use of informal value transfer system"),
    MULTIPLE_LOCATIONS(910, 9, "Suspicious use of multiple locations"),
    TWO_OR_MORE_INDIVIDUALS(911, 9, "Two or more individuals working together"),
    UNLICENSED_MSB(913, 9, "Unlicensed or unregistered MSB"),
    COUNTERFEIT_INSTRUMENT(917, 9, "Counterfeit instrument (other)"),
    ACCOUNT_TAKEOVER(920, 9, "Account takeover"),
    ELDER_FINANCIAL_EXPLOITATION(921, 9, "Elder financial exploitation");

    private final int subtypeId;
    private final int typeId;
    private final String description;

    ActivityTypeCode(int subtypeId, int typeId, String description) {
        this.subtypeId = subtypeId;
        this.typeId = typeId;
        this.description = description;
    }

    /** @deprecated Use getSubtypeId() */
    @Deprecated
    public int getCode() {
        return subtypeId;
    }

    public int getSubtypeId() {
        return subtypeId;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getDescription() {
        return description;
    }
}
