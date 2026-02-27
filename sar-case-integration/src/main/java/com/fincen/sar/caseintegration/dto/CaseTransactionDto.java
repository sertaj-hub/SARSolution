package com.fincen.sar.caseintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseTransactionDto {
    private String transactionId;
    private String alertId;
    private String accountId;
    private LocalDate transactionDate;
    private String transactionType;
    private BigDecimal amount;
    private String currencyCode;
    private String direction; // CREDIT, DEBIT
    private String counterpartyName;
    private String counterpartyAccount;
    private String counterpartyInstitution;
    private String counterpartyCountry;
    private String locationType;
    private String referenceNumber;
    private Boolean structuringFlag;
    private Boolean rapidMovementFlag;
    private Boolean unusualPatternFlag;
    private String suspicionNotes;
}
