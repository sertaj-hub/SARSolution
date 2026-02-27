package com.fincen.sar.caseintegration.client;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CaseSearchCriteria {
    private String caseNumber;
    private String alertId;
    private String subjectName;
    private String accountNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String status;
    private String assignedAnalyst;
    private int page;
    private int size;
}
