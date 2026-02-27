package com.fincen.sar.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SAR Account information linked to a subject.
 * Corresponds to the Account element in BSA XML 2.0 schema.
 * Auto-populated from AML case management system.
 */
@Entity
@Table(name = "sar_accounts")
@Audited
@Getter
@Setter
public class SarAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sar_report_id", nullable = false)
    private SarReport sarReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private SarSubject subject;

    // Source auto-population
    @Column(name = "case_account_id")
    private String caseAccountId;

    @Column(name = "auto_populated")
    private Boolean autoPopulated = false;

    // --- Account Identification ---

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_number_closed")
    private Boolean accountNumberClosed = false;

    @Column(name = "account_type")
    private String accountType; // Checking, Savings, Loan, etc.

    @Column(name = "product_type")
    private String productType; // Per FinCEN product type codes

    // --- Institution ---

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "institution_ein")
    private String institutionEin;

    @Column(name = "routing_number")
    private String routingNumber;

    // --- Account Details ---

    @Column(name = "opened_date")
    private LocalDate openedDate;

    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency_code")
    private String currencyCode = "USD";

    // --- Foreign Account ---

    @Column(name = "is_foreign_account")
    private Boolean isForeignAccount = false;

    @Column(name = "foreign_country")
    private String foreignCountry;

    // --- Status ---

    @Column(name = "action_account_closed")
    private Boolean actionAccountClosed = false;

    @Column(name = "action_account_frozen")
    private Boolean actionAccountFrozen = false;

    @Column(name = "no_action_taken")
    private Boolean noActionTaken = true;
}
