package com.fincen.sar.core.domain;

import com.fincen.sar.core.enums.FilingStatus;
import com.fincen.sar.core.enums.SarStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root aggregate for a FinCEN SAR (Suspicious Activity Report).
 * Corresponds to the BSA XML 'Activity' element and FinCEN SAR form parts I-V.
 */
@Entity
@Table(name = "sar_reports")
@Audited
@Getter
@Setter
public class SarReport extends BaseEntity {

    // --- Identifiers ---

    @Column(name = "report_number", unique = true, updatable = false)
    private String reportNumber; // Internal tracking number (auto-generated)

    @Column(name = "case_id")
    private String caseId; // Linked AML case management ID

    @Column(name = "case_system")
    private String caseSystem; // Source case management system name

    @Column(name = "prior_bsa_identifier")
    private String priorBsaIdentifier; // For amendments - prior BSA filing ID

    @Column(name = "bsa_identifier")
    private String bsaIdentifier; // Assigned by FinCEN upon acceptance

    // --- Status ---

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private SarStatus status = SarStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status")
    private FilingStatus filingStatus;

    // --- Activity Dates (Part II) ---

    @Column(name = "activity_from_date")
    private LocalDate activityFromDate;

    @Column(name = "activity_to_date")
    private LocalDate activityToDate;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    // --- Flags ---

    @Column(name = "continuing_activity")
    private Boolean continuingActivity = false;

    @Column(name = "corrects_amends_prior")
    private Boolean correctsAmendsPrior = false;

    @Column(name = "joint_report")
    private Boolean jointReport = false;

    @Column(name = "fi_noted_suspicious_activity")
    private Boolean fiNotedSuspiciousActivity = true;

    // --- Financial Summary (Part II) ---

    @Column(name = "total_suspicious_amount", precision = 19, scale = 2)
    private BigDecimal totalSuspiciousAmount;

    @Column(name = "no_amount_involved")
    private Boolean noAmountInvolved = false;

    // --- Narrative (Part V) ---

    @Column(name = "narrative", columnDefinition = "TEXT")
    private String narrative;

    // --- eFiling tracking ---

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "fincen_tracking_number")
    private String fincenTrackingNumber;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // --- Filing Institution (Part III) ---

    @Column(name = "filing_institution_name")
    @NotBlank
    private String filingInstitutionName;

    @Column(name = "filing_institution_ein")
    private String filingInstitutionEin;

    @Column(name = "filing_institution_type")
    private String filingInstitutionType; // e.g. Bank, MSB, Casino

    @Column(name = "contact_office_name")
    private String contactOfficeName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    // --- Relationships ---

    @OneToMany(mappedBy = "sarReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SarSubject> subjects = new ArrayList<>();

    @OneToMany(mappedBy = "sarReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SarAccount> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "sarReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SarTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "sarReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SarActivityType> activityTypes = new ArrayList<>();

    @OneToMany(mappedBy = "sarReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SarBranch> branches = new ArrayList<>();

    // --- Helper methods ---

    public void addSubject(SarSubject subject) {
        subject.setSarReport(this);
        subjects.add(subject);
    }

    public void addAccount(SarAccount account) {
        account.setSarReport(this);
        accounts.add(account);
    }

    public void addTransaction(SarTransaction transaction) {
        transaction.setSarReport(this);
        transactions.add(transaction);
    }

    public void addActivityType(SarActivityType activityType) {
        activityType.setSarReport(this);
        activityTypes.add(activityType);
    }

    public void addBranch(SarBranch branch) {
        branch.setSarReport(this);
        branches.add(branch);
    }

    public boolean isAmendment() {
        return Boolean.TRUE.equals(correctsAmendsPrior) && priorBsaIdentifier != null;
    }

    public boolean isReadyForFiling() {
        return status == SarStatus.APPROVED && !subjects.isEmpty() && narrative != null;
    }
}
