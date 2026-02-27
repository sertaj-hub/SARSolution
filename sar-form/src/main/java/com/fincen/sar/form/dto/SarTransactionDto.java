package com.fincen.sar.form.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class SarTransactionDto {
    private UUID id;
    private UUID accountId;
    private String caseTransactionId;
    private String alertId;
    private Boolean autoPopulated;
    private LocalDate transactionDate;
    private String transactionType;
    private BigDecimal amount;
    private String currencyCode;
    private String direction;
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
