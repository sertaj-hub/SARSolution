package com.fincen.sar.form.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class SarAccountDto {
    private UUID id;
    private UUID subjectId;
    private String caseAccountId;
    private Boolean autoPopulated;
    private String accountNumber;
    private Boolean accountNumberClosed;
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
    private Boolean actionAccountClosed;
    private Boolean actionAccountFrozen;
    private Boolean noActionTaken;
}
