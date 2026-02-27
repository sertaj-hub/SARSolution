package com.fincen.sar.form.dto;

import com.fincen.sar.core.enums.SarStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class SarReportDto {
    private UUID id;
    private String reportNumber;
    private String caseId;
    private String caseSystem;
    private SarStatus status;
    private String filingStatus;
    private LocalDate activityFromDate;
    private LocalDate activityToDate;
    private LocalDate filingDate;
    private Boolean continuingActivity;
    private Boolean correctsAmendsPrior;
    private Boolean jointReport;
    private Boolean fiNotedSuspiciousActivity;
    private BigDecimal totalSuspiciousAmount;
    private Boolean noAmountInvolved;
    private String narrative;
    private String filingInstitutionName;
    private String filingInstitutionEin;
    private String filingInstitutionType;
    private String contactOfficeName;
    private String contactPhone;
    private String contactEmail;
    private String priorBsaIdentifier;
    private String bsaIdentifier;
    private String fincenTrackingNumber;
    private LocalDateTime submittedAt;
    private LocalDateTime acknowledgedAt;
    private List<SarSubjectDto> subjects;
    private List<SarAccountDto> accounts;
    private List<SarTransactionDto> transactions;
    private List<SarActivityTypeDto> activityTypes;
    private List<SarBranchDto> branches;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
