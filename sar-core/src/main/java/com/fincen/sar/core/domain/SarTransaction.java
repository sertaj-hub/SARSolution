package com.fincen.sar.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Suspicious transaction records linked to a SAR report.
 * Auto-populated from AML case management system.
 * Transactions form the basis of suspicious activity analysis.
 */
@Entity
@Table(name = "sar_transactions")
@Audited
@Getter
@Setter
public class SarTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sar_report_id", nullable = false)
    private SarReport sarReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private SarAccount account;

    // Source auto-population
    @Column(name = "case_transaction_id")
    private String caseTransactionId;

    @Column(name = "alert_id")
    private String alertId;

    @Column(name = "auto_populated")
    private Boolean autoPopulated = false;

    // --- Transaction Details ---

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "transaction_datetime")
    private LocalDateTime transactionDatetime;

    @Column(name = "transaction_type")
    private String transactionType; // Wire, ACH, Cash, Check, etc.

    @Column(name = "transaction_type_other")
    private String transactionTypeOther;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code")
    private String currencyCode = "USD";

    @Column(name = "currency_foreign")
    private String currencyForeign;

    // --- Direction ---

    @Column(name = "direction")
    private String direction; // CREDIT, DEBIT

    // --- Counterparty ---

    @Column(name = "counterparty_name")
    private String counterpartyName;

    @Column(name = "counterparty_account")
    private String counterpartyAccount;

    @Column(name = "counterparty_institution")
    private String counterpartyInstitution;

    @Column(name = "counterparty_country")
    private String counterpartyCountry;

    // --- Location ---

    @Column(name = "location_type")
    private String locationType; // Branch, ATM, Online, etc.

    @Column(name = "location_description")
    private String locationDescription;

    // --- Suspicion Flags ---

    @Column(name = "structuring_flag")
    private Boolean structuringFlag = false;

    @Column(name = "rapid_movement_flag")
    private Boolean rapidMovementFlag = false;

    @Column(name = "unusual_pattern_flag")
    private Boolean unusualPatternFlag = false;

    @Column(name = "suspicion_notes", columnDefinition = "TEXT")
    private String suspicionNotes;

    // --- Reference ---

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "check_number")
    private String checkNumber;
}
