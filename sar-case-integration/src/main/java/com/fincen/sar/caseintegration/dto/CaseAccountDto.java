package com.fincen.sar.caseintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseAccountDto {
    private String accountId;
    private String subjectId; // Linked subject
    private String accountNumber;
    private String accountType;
    private String productType;
    private String institutionName;
    private String institutionEin;
    private String routingNumber;
    private LocalDate openedDate;
    private LocalDate closedDate;
    private BigDecimal balance;
    private String currencyCode;
    private Boolean isForeignAccount;
    private String foreignCountry;
    private Boolean closed;
}
