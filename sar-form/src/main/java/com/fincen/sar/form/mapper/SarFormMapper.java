package com.fincen.sar.form.mapper;

import com.fincen.sar.core.domain.*;
import com.fincen.sar.form.dto.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SarFormMapper {

    @Mapping(target = "subjects", source = "subjects")
    @Mapping(target = "accounts", source = "accounts")
    @Mapping(target = "transactions", source = "transactions")
    @Mapping(target = "activityTypes", source = "activityTypes")
    @Mapping(target = "branches", source = "branches")
    SarReportDto toDto(SarReport report);

    List<SarReportDto> toDtoList(List<SarReport> reports);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reportNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "subjects", ignore = true)
    @Mapping(target = "accounts", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "activityTypes", ignore = true)
    @Mapping(target = "branches", ignore = true)
    SarReport fromCreateRequest(CreateSarRequest request);

    SarSubjectDto subjectToDto(SarSubject subject);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sarReport", ignore = true)
    SarSubject subjectFromDto(SarSubjectDto dto);

    @Mapping(target = "subjectId", source = "subject.id")
    SarAccountDto accountToDto(SarAccount account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sarReport", ignore = true)
    @Mapping(target = "subject", ignore = true)
    SarAccount accountFromDto(SarAccountDto dto);

    @Mapping(target = "accountId", source = "account.id")
    SarTransactionDto transactionToDto(SarTransaction transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sarReport", ignore = true)
    @Mapping(target = "account", ignore = true)
    SarTransaction transactionFromDto(SarTransactionDto dto);

    SarActivityTypeDto activityTypeToDto(SarActivityType activityType);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sarReport", ignore = true)
    SarActivityType activityTypeFromDto(SarActivityTypeDto dto);

    SarBranchDto branchToDto(SarBranch branch);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sarReport", ignore = true)
    SarBranch branchFromDto(SarBranchDto dto);
}
