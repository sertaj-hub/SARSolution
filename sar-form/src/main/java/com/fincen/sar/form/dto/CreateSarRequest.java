package com.fincen.sar.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSarRequest {

    @NotBlank(message = "Filing institution name is required")
    private String filingInstitutionName;

    private String filingInstitutionEin;
    private String filingInstitutionType;
    private String contactOfficeName;
    private String contactPhone;
    private String contactEmail;
    private String caseId;
    private String caseSystem;
    private LocalDate activityFromDate;
    private LocalDate activityToDate;
    private BigDecimal totalSuspiciousAmount;
    private Boolean continuingActivity = false;
    private Boolean jointReport = false;

    @Size(max = 20000, message = "Narrative cannot exceed 20000 characters")
    private String narrative;

    // Whether to auto-populate from linked case immediately
    private Boolean autoPopulateFromCase = false;
}
