package com.fincen.sar.form.service;

import com.fincen.sar.caseintegration.service.CasePopulationService;
import com.fincen.sar.core.domain.*;
import com.fincen.sar.core.enums.SarStatus;
import com.fincen.sar.core.exception.SarNotFoundException;
import com.fincen.sar.core.exception.SarValidationException;
import com.fincen.sar.core.repository.*;
import com.fincen.sar.core.util.ReportNumberGenerator;
import com.fincen.sar.form.dto.*;
import com.fincen.sar.form.mapper.SarFormMapper;
import com.fincen.sar.form.validator.SarFormValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Core service for SAR form lifecycle management.
 * Handles creation, editing, status transitions, and case integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SarFormService {

    private final SarReportRepository sarReportRepository;
    private final SarSubjectRepository subjectRepository;
    private final SarAccountRepository accountRepository;
    private final SarTransactionRepository transactionRepository;
    private final SarActivityTypeRepository activityTypeRepository;
    private final SarBranchRepository branchRepository;
    private final SarFormMapper mapper;
    private final SarFormValidator validator;
    private final ReportNumberGenerator reportNumberGenerator;
    private final CasePopulationService casePopulationService;

    /**
     * Create a new SAR report in DRAFT status.
     */
    public SarReportDto createSarReport(CreateSarRequest request) {
        SarReport report = mapper.fromCreateRequest(request);
        report.setReportNumber(reportNumberGenerator.generate());
        report.setStatus(SarStatus.DRAFT);
        report.setFilingDate(LocalDate.now());

        report = sarReportRepository.save(report);
        log.info("Created SAR report: {}", report.getReportNumber());

        // Auto-populate from linked case if requested
        if (Boolean.TRUE.equals(request.getAutoPopulateFromCase())
                && request.getCaseId() != null && !request.getCaseId().isBlank()) {
            try {
                casePopulationService.populateFromCase(report.getId(), request.getCaseId());
                report = sarReportRepository.findById(report.getId()).orElseThrow();
            } catch (Exception e) {
                log.warn("Auto-population from case {} failed: {}", request.getCaseId(), e.getMessage());
            }
        }

        return mapper.toDto(report);
    }

    /**
     * Get SAR report by ID.
     */
    @Transactional(readOnly = true)
    public SarReportDto getSarReport(UUID id) {
        return mapper.toDto(sarReportRepository.findById(id)
                .orElseThrow(() -> new SarNotFoundException(id)));
    }

    /**
     * Get SAR report by report number.
     */
    @Transactional(readOnly = true)
    public SarReportDto getSarReportByNumber(String reportNumber) {
        return mapper.toDto(sarReportRepository.findByReportNumber(reportNumber)
                .orElseThrow(() -> new SarNotFoundException(reportNumber)));
    }

    /**
     * List SAR reports with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SarReportDto> listSarReports(SarStatus status, Pageable pageable) {
        Page<SarReport> page = status != null
                ? sarReportRepository.findByStatus(status, pageable)
                : sarReportRepository.findAll(pageable);
        return page.map(mapper::toDto);
    }

    /**
     * Update SAR report fields (only in DRAFT or IN_REVIEW status).
     */
    public SarReportDto updateSarReport(UUID id, SarReportDto updateDto) {
        SarReport report = sarReportRepository.findById(id)
                .orElseThrow(() -> new SarNotFoundException(id));

        validateEditableStatus(report);

        // Update basic fields
        if (updateDto.getFilingInstitutionName() != null) report.setFilingInstitutionName(updateDto.getFilingInstitutionName());
        if (updateDto.getFilingInstitutionEin() != null) report.setFilingInstitutionEin(updateDto.getFilingInstitutionEin());
        if (updateDto.getFilingInstitutionType() != null) report.setFilingInstitutionType(updateDto.getFilingInstitutionType());
        if (updateDto.getContactOfficeName() != null) report.setContactOfficeName(updateDto.getContactOfficeName());
        if (updateDto.getContactPhone() != null) report.setContactPhone(updateDto.getContactPhone());
        if (updateDto.getContactEmail() != null) report.setContactEmail(updateDto.getContactEmail());
        if (updateDto.getActivityFromDate() != null) report.setActivityFromDate(updateDto.getActivityFromDate());
        if (updateDto.getActivityToDate() != null) report.setActivityToDate(updateDto.getActivityToDate());
        if (updateDto.getTotalSuspiciousAmount() != null) report.setTotalSuspiciousAmount(updateDto.getTotalSuspiciousAmount());
        if (updateDto.getNoAmountInvolved() != null) report.setNoAmountInvolved(updateDto.getNoAmountInvolved());
        if (updateDto.getNarrative() != null) report.setNarrative(updateDto.getNarrative());
        if (updateDto.getContinuingActivity() != null) report.setContinuingActivity(updateDto.getContinuingActivity());
        if (updateDto.getJointReport() != null) report.setJointReport(updateDto.getJointReport());
        if (updateDto.getFilingDate() != null) report.setFilingDate(updateDto.getFilingDate());
        if (updateDto.getPriorBsaIdentifier() != null) report.setPriorBsaIdentifier(updateDto.getPriorBsaIdentifier());
        if (updateDto.getCorrectsAmendsPrior() != null) report.setCorrectsAmendsPrior(updateDto.getCorrectsAmendsPrior());

        report = sarReportRepository.save(report);
        log.info("Updated SAR report: {}", report.getReportNumber());
        return mapper.toDto(report);
    }

    /**
     * Submit SAR for compliance review (DRAFT -> IN_REVIEW).
     */
    public SarReportDto submitForReview(UUID id) {
        SarReport report = sarReportRepository.findById(id)
                .orElseThrow(() -> new SarNotFoundException(id));

        if (report.getStatus() != SarStatus.DRAFT) {
            throw new SarValidationException(
                    List.of("SAR must be in DRAFT status to submit for review. Current status: " + report.getStatus()));
        }

        validator.validateForReview(report);
        report.setStatus(SarStatus.IN_REVIEW);
        return mapper.toDto(sarReportRepository.save(report));
    }

    /**
     * Approve SAR for eFiling (IN_REVIEW -> APPROVED).
     */
    public SarReportDto approveSar(UUID id) {
        SarReport report = sarReportRepository.findById(id)
                .orElseThrow(() -> new SarNotFoundException(id));

        if (report.getStatus() != SarStatus.IN_REVIEW) {
            throw new SarValidationException(
                    List.of("SAR must be in IN_REVIEW status to approve. Current status: " + report.getStatus()));
        }

        validator.validateForReview(report);
        report.setStatus(SarStatus.APPROVED);
        log.info("Approved SAR report {} for eFiling", report.getReportNumber());
        return mapper.toDto(sarReportRepository.save(report));
    }

    /**
     * Reject SAR back to DRAFT with reason.
     */
    public SarReportDto rejectSar(UUID id, String rejectionReason) {
        SarReport report = sarReportRepository.findById(id)
                .orElseThrow(() -> new SarNotFoundException(id));

        if (report.getStatus() != SarStatus.IN_REVIEW) {
            throw new SarValidationException(
                    List.of("Only IN_REVIEW SARs can be rejected. Current status: " + report.getStatus()));
        }

        report.setStatus(SarStatus.DRAFT);
        report.setRejectionReason(rejectionReason);
        return mapper.toDto(sarReportRepository.save(report));
    }

    /**
     * Auto-populate SAR from AML case management system.
     */
    public SarReportDto populateFromCase(UUID sarId, String caseId) {
        casePopulationService.populateFromCase(sarId, caseId);
        return getSarReport(sarId);
    }

    // --- Subject management ---

    public SarSubjectDto addSubject(UUID sarId, SarSubjectDto subjectDto) {
        SarReport report = getEditableReport(sarId);
        SarSubject subject = mapper.subjectFromDto(subjectDto);
        subject.setSeqNum(report.getSubjects().size() + 1);
        report.addSubject(subject);
        sarReportRepository.save(report);
        return mapper.subjectToDto(subject);
    }

    public SarSubjectDto updateSubject(UUID sarId, UUID subjectId, SarSubjectDto subjectDto) {
        getEditableReport(sarId);
        SarSubject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SarNotFoundException(subjectId));
        // Update fields
        if (subjectDto.getFirstName() != null) subject.setFirstName(subjectDto.getFirstName());
        if (subjectDto.getLastName() != null) subject.setLastName(subjectDto.getLastName());
        if (subjectDto.getMiddleName() != null) subject.setMiddleName(subjectDto.getMiddleName());
        if (subjectDto.getDateOfBirth() != null) subject.setDateOfBirth(subjectDto.getDateOfBirth());
        if (subjectDto.getIdType() != null) subject.setIdType(subjectDto.getIdType());
        if (subjectDto.getIdNumber() != null) subject.setIdNumber(subjectDto.getIdNumber());
        if (subjectDto.getAddress() != null) subject.setAddress(subjectDto.getAddress());
        if (subjectDto.getCity() != null) subject.setCity(subjectDto.getCity());
        if (subjectDto.getState() != null) subject.setState(subjectDto.getState());
        if (subjectDto.getZipCode() != null) subject.setZipCode(subjectDto.getZipCode());
        if (subjectDto.getOccupation() != null) subject.setOccupation(subjectDto.getOccupation());
        if (subjectDto.getRoleCode() != null) subject.setRoleCode(subjectDto.getRoleCode());
        if (subjectDto.getRoleOtherDescription() != null) subject.setRoleOtherDescription(subjectDto.getRoleOtherDescription());
        return mapper.subjectToDto(subjectRepository.save(subject));
    }

    public void removeSubject(UUID sarId, UUID subjectId) {
        SarReport report = getEditableReport(sarId);
        report.getSubjects().removeIf(s -> s.getId().equals(subjectId));
        sarReportRepository.save(report);
    }

    // --- Account management ---

    public SarAccountDto addAccount(UUID sarId, SarAccountDto accountDto) {
        SarReport report = getEditableReport(sarId);
        SarAccount account = mapper.accountFromDto(accountDto);
        if (accountDto.getSubjectId() != null) {
            subjectRepository.findById(accountDto.getSubjectId()).ifPresent(account::setSubject);
        }
        report.addAccount(account);
        sarReportRepository.save(report);
        return mapper.accountToDto(account);
    }

    public void removeAccount(UUID sarId, UUID accountId) {
        SarReport report = getEditableReport(sarId);
        report.getAccounts().removeIf(a -> a.getId().equals(accountId));
        sarReportRepository.save(report);
    }

    // --- Transaction management ---

    public SarTransactionDto addTransaction(UUID sarId, SarTransactionDto txnDto) {
        SarReport report = getEditableReport(sarId);
        SarTransaction txn = mapper.transactionFromDto(txnDto);
        if (txnDto.getAccountId() != null) {
            accountRepository.findById(txnDto.getAccountId()).ifPresent(txn::setAccount);
        }
        report.addTransaction(txn);
        sarReportRepository.save(report);
        return mapper.transactionToDto(txn);
    }

    public void removeTransaction(UUID sarId, UUID txnId) {
        SarReport report = getEditableReport(sarId);
        report.getTransactions().removeIf(t -> t.getId().equals(txnId));
        sarReportRepository.save(report);
    }

    // --- Activity type management ---

    public SarActivityTypeDto addActivityType(UUID sarId, SarActivityTypeDto dto) {
        SarReport report = getEditableReport(sarId);
        SarActivityType activityType = mapper.activityTypeFromDto(dto);
        activityType.setSeqNum(report.getActivityTypes().size() + 1);
        report.addActivityType(activityType);
        sarReportRepository.save(report);
        return mapper.activityTypeToDto(activityType);
    }

    public SarActivityTypeDto updateActivityType(UUID sarId, UUID activityTypeId, SarActivityTypeDto dto) {
        getEditableReport(sarId);
        SarActivityType activityType = activityTypeRepository.findById(activityTypeId)
                .orElseThrow(() -> new SarNotFoundException(activityTypeId));
        if (dto.getActivityTypeCode() != null) activityType.setActivityTypeCode(dto.getActivityTypeCode());
        if (dto.getActivityTypeOther() != null) activityType.setActivityTypeOther(dto.getActivityTypeOther());
        if (dto.getAmount() != null) activityType.setAmount(dto.getAmount());
        if (dto.getProductType() != null) activityType.setProductType(dto.getProductType());
        if (dto.getProductInstrumentDescription() != null) activityType.setProductInstrumentDescription(dto.getProductInstrumentDescription());
        return mapper.activityTypeToDto(activityTypeRepository.save(activityType));
    }

    public void removeActivityType(UUID sarId, UUID activityTypeId) {
        SarReport report = getEditableReport(sarId);
        report.getActivityTypes().removeIf(at -> at.getId().equals(activityTypeId));
        sarReportRepository.save(report);
    }

    // --- Branch management ---

    public SarBranchDto addBranch(UUID sarId, SarBranchDto dto) {
        SarReport report = getEditableReport(sarId);
        SarBranch branch = mapper.branchFromDto(dto);
        branch.setSeqNum(report.getBranches().size() + 1);
        report.addBranch(branch);
        sarReportRepository.save(report);
        return mapper.branchToDto(branch);
    }

    public SarBranchDto updateBranch(UUID sarId, UUID branchId, SarBranchDto dto) {
        getEditableReport(sarId);
        SarBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new SarNotFoundException(branchId));
        if (dto.getBranchName() != null) branch.setBranchName(dto.getBranchName());
        if (dto.getRssdNumber() != null) branch.setRssdNumber(dto.getRssdNumber());
        if (dto.getAddress() != null) branch.setAddress(dto.getAddress());
        if (dto.getCity() != null) branch.setCity(dto.getCity());
        if (dto.getState() != null) branch.setState(dto.getState());
        if (dto.getZipCode() != null) branch.setZipCode(dto.getZipCode());
        if (dto.getCountry() != null) branch.setCountry(dto.getCountry());
        if (dto.getIsPrimary() != null) branch.setIsPrimary(dto.getIsPrimary());
        return mapper.branchToDto(branchRepository.save(branch));
    }

    public void removeBranch(UUID sarId, UUID branchId) {
        SarReport report = getEditableReport(sarId);
        report.getBranches().removeIf(b -> b.getId().equals(branchId));
        sarReportRepository.save(report);
    }

    private SarReport getEditableReport(UUID id) {
        SarReport report = sarReportRepository.findById(id)
                .orElseThrow(() -> new SarNotFoundException(id));
        validateEditableStatus(report);
        return report;
    }

    private void validateEditableStatus(SarReport report) {
        if (report.getStatus() == SarStatus.SUBMITTED
                || report.getStatus() == SarStatus.ACKNOWLEDGED
                || report.getStatus() == SarStatus.CLOSED) {
            throw new SarValidationException(
                    List.of("SAR report cannot be edited in status: " + report.getStatus()
                            + ". Create an amendment to make changes."));
        }
    }
}
