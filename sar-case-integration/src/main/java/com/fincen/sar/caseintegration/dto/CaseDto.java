package com.fincen.sar.caseintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AML Case Management System case data DTO.
 * Used for auto-populating SAR forms from case data.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDto {
    private String caseId;
    private String caseNumber;
    private String caseType; // SAR, CTR, INVESTIGATION, etc.
    private String caseStatus;
    private String casePriority;
    private String assignedAnalyst;
    private String assignedTeam;
    private LocalDateTime openedDate;
    private LocalDateTime closedDate;
    private String institution;
    private String institutionEin;
    private String description;
    private String disposition;

    // Alert info (can trigger SAR)
    private String alertId;
    private String alertType;
    private String alertScore;

    // Pre-filled SAR data
    private List<CaseSubjectDto> subjects;
    private List<CaseAccountDto> accounts;
    private List<CaseTransactionDto> transactions;

    // Suspicious activity summary
    private String suspiciousActivitySummary;
    private String activityFromDate;
    private String activityToDate;
    private java.math.BigDecimal totalSuspiciousAmount;
    private List<String> activityTypes; // Suspicious activity type codes
}
