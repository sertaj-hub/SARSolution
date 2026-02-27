package com.fincen.sar.caseintegration.mapper;

import com.fincen.sar.caseintegration.dto.CaseAccountDto;
import com.fincen.sar.caseintegration.dto.CaseDto;
import com.fincen.sar.caseintegration.dto.CaseSubjectDto;
import com.fincen.sar.caseintegration.dto.CaseTransactionDto;
import com.fincen.sar.core.domain.SarAccount;
import com.fincen.sar.core.domain.SarReport;
import com.fincen.sar.core.domain.SarSubject;
import com.fincen.sar.core.domain.SarTransaction;
import com.fincen.sar.core.enums.ActivityTypeCode;
import com.fincen.sar.core.enums.IdentificationTypeCode;
import com.fincen.sar.core.enums.SubjectRoleCode;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Maps AML Case Management data to SAR domain objects.
 * Used for auto-population of SAR form from case data.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseToSarMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseSubjectId", source = "subjectId")
    @Mapping(target = "autoPopulated", constant = "true")
    @Mapping(target = "isEntity", source = "isEntity")
    @Mapping(target = "idType", source = "idType", qualifiedByName = "mapIdType")
    @Mapping(target = "roleCode", source = "role", qualifiedByName = "mapRole")
    @Mapping(target = "sarReport", ignore = true)
    SarSubject caseSubjectToSarSubject(CaseSubjectDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseAccountId", source = "accountId")
    @Mapping(target = "autoPopulated", constant = "true")
    @Mapping(target = "accountNumberClosed", source = "closed")
    @Mapping(target = "sarReport", ignore = true)
    @Mapping(target = "subject", ignore = true)
    SarAccount caseAccountToSarAccount(CaseAccountDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseTransactionId", source = "transactionId")
    @Mapping(target = "autoPopulated", constant = "true")
    @Mapping(target = "sarReport", ignore = true)
    @Mapping(target = "account", ignore = true)
    SarTransaction caseTransactionToSarTransaction(CaseTransactionDto dto);

    @Named("mapIdType")
    default IdentificationTypeCode mapIdType(String idType) {
        if (idType == null) return null;
        return switch (idType.toUpperCase()) {
            case "SSN", "ITIN", "SSN_ITIN" -> IdentificationTypeCode.SSN_ITIN;
            case "EIN" -> IdentificationTypeCode.EIN;
            case "PASSPORT" -> IdentificationTypeCode.PASSPORT;
            case "DRIVERS_LICENSE", "DL", "STATE_ID" -> IdentificationTypeCode.DRIVERS_LICENSE;
            case "ALIEN_REGISTRATION" -> IdentificationTypeCode.ALIEN_REGISTRATION;
            case "FOREIGN_ID" -> IdentificationTypeCode.FOREIGN_ID;
            case "NATIONAL_ID" -> IdentificationTypeCode.NATIONAL_ID;
            default -> IdentificationTypeCode.OTHER;
        };
    }

    @Named("mapRole")
    default SubjectRoleCode mapRole(String role) {
        if (role == null) return SubjectRoleCode.OTHER;
        try {
            return SubjectRoleCode.valueOf(role.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return SubjectRoleCode.OTHER;
        }
    }

    @Named("mapActivityType")
    default ActivityTypeCode mapActivityType(String type) {
        if (type == null) return null;
        try {
            return ActivityTypeCode.valueOf(type.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return ActivityTypeCode.FRAUD_OTHER;
        }
    }

    default LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
