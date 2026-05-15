package com.fincen.sar.api.controller;

import com.fincen.sar.audit.dto.AuditLogDto;
import com.fincen.sar.audit.dto.RevisionHistoryDto;
import com.fincen.sar.audit.service.AuditService;
import com.fincen.sar.core.enums.SarStatus;
import com.fincen.sar.form.dto.*;
import com.fincen.sar.form.service.SarFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for SAR Report lifecycle management.
 * Provides CRUD operations, status transitions, and case integration.
 */
@RestController
@RequestMapping("/api/v1/sar-reports")
@RequiredArgsConstructor
@Tag(name = "SAR Reports", description = "FinCEN Suspicious Activity Report management")
public class SarReportController {

    private final SarFormService sarFormService;
    private final AuditService auditService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Create new SAR report",
            description = "Creates a new SAR in DRAFT status. Optionally auto-populates from AML case.")
    @ApiResponse(responseCode = "201", description = "SAR report created")
    public ResponseEntity<SarReportDto> createSarReport(
            @Valid @RequestBody CreateSarRequest request) {
        SarReportDto created = sarFormService.createSarReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get SAR report by ID")
    public ResponseEntity<SarReportDto> getSarReport(@PathVariable UUID id) {
        return ResponseEntity.ok(sarFormService.getSarReport(id));
    }

    @GetMapping("/by-number/{reportNumber}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get SAR report by report number")
    public ResponseEntity<SarReportDto> getSarReportByNumber(@PathVariable String reportNumber) {
        return ResponseEntity.ok(sarFormService.getSarReportByNumber(reportNumber));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN', 'VIEWER')")
    @Operation(summary = "List SAR reports with optional status filter and pagination")
    public ResponseEntity<Page<SarReportDto>> listSarReports(
            @RequestParam(required = false) SarStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(sarFormService.listSarReports(status, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Update SAR report fields")
    public ResponseEntity<SarReportDto> updateSarReport(
            @PathVariable UUID id,
            @RequestBody SarReportDto updateDto) {
        return ResponseEntity.ok(sarFormService.updateSarReport(id, updateDto));
    }

    // --- Status Transitions ---

    @PostMapping("/{id}/submit-for-review")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(summary = "Submit SAR for compliance review (DRAFT -> IN_REVIEW)",
            description = "Validates completeness per FinCEN SAR form requirements before transition")
    public ResponseEntity<SarReportDto> submitForReview(@PathVariable UUID id) {
        SarReportDto result = sarFormService.submitForReview(id);
        auditService.logStatusChange(id, "DRAFT", "IN_REVIEW", "Submitted for compliance review");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Approve SAR for eFiling (IN_REVIEW -> APPROVED)")
    public ResponseEntity<SarReportDto> approveSar(@PathVariable UUID id) {
        SarReportDto result = sarFormService.approveSar(id);
        auditService.logStatusChange(id, "IN_REVIEW", "APPROVED", "Approved for eFiling");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Reject SAR back to DRAFT with reason")
    public ResponseEntity<SarReportDto> rejectSar(
            @PathVariable UUID id,
            @RequestParam String reason) {
        SarReportDto result = sarFormService.rejectSar(id, reason);
        auditService.logStatusChange(id, "IN_REVIEW", "DRAFT", "Rejected: " + reason);
        return ResponseEntity.ok(result);
    }

    // --- Case Integration ---

    @PostMapping("/{id}/populate-from-case")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Auto-populate SAR from AML case management system",
            description = "Fetches subjects, accounts, and transactions from the linked case system")
    public ResponseEntity<SarReportDto> populateFromCase(
            @PathVariable UUID id,
            @Parameter(description = "Case ID in the AML system") @RequestParam String caseId) {
        SarReportDto result = sarFormService.populateFromCase(id, caseId);
        auditService.logSarEvent(id, "CASE_POPULATE",
                "Auto-populated from case: " + caseId);
        return ResponseEntity.ok(result);
    }

    // --- Subject Management ---

    @PostMapping("/{id}/subjects")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Add subject to SAR (Part I)")
    public ResponseEntity<SarSubjectDto> addSubject(
            @PathVariable UUID id,
            @RequestBody SarSubjectDto subjectDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sarFormService.addSubject(id, subjectDto));
    }

    @PutMapping("/{id}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Update subject information")
    public ResponseEntity<SarSubjectDto> updateSubject(
            @PathVariable UUID id,
            @PathVariable UUID subjectId,
            @RequestBody SarSubjectDto subjectDto) {
        return ResponseEntity.ok(sarFormService.updateSubject(id, subjectId, subjectDto));
    }

    @DeleteMapping("/{id}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Remove subject from SAR")
    public ResponseEntity<Void> removeSubject(
            @PathVariable UUID id,
            @PathVariable UUID subjectId) {
        sarFormService.removeSubject(id, subjectId);
        return ResponseEntity.noContent().build();
    }

    // --- Account Management ---

    @PostMapping("/{id}/accounts")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Add account to SAR")
    public ResponseEntity<SarAccountDto> addAccount(
            @PathVariable UUID id,
            @RequestBody SarAccountDto accountDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sarFormService.addAccount(id, accountDto));
    }

    @DeleteMapping("/{id}/accounts/{accountId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Remove account from SAR")
    public ResponseEntity<Void> removeAccount(
            @PathVariable UUID id,
            @PathVariable UUID accountId) {
        sarFormService.removeAccount(id, accountId);
        return ResponseEntity.noContent().build();
    }

    // --- Transaction Management ---

    @PostMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Add suspicious transaction to SAR")
    public ResponseEntity<SarTransactionDto> addTransaction(
            @PathVariable UUID id,
            @RequestBody SarTransactionDto txnDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sarFormService.addTransaction(id, txnDto));
    }

    @DeleteMapping("/{id}/transactions/{txnId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Remove transaction from SAR")
    public ResponseEntity<Void> removeTransaction(
            @PathVariable UUID id,
            @PathVariable UUID txnId) {
        sarFormService.removeTransaction(id, txnId);
        return ResponseEntity.noContent().build();
    }

    // --- Activity Type Management ---

    @PostMapping("/{id}/activity-types")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Add suspicious activity type to SAR (Part II)")
    public ResponseEntity<SarActivityTypeDto> addActivityType(
            @PathVariable UUID id,
            @RequestBody SarActivityTypeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sarFormService.addActivityType(id, dto));
    }

    @PutMapping("/{id}/activity-types/{activityTypeId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Update a suspicious activity type")
    public ResponseEntity<SarActivityTypeDto> updateActivityType(
            @PathVariable UUID id,
            @PathVariable UUID activityTypeId,
            @RequestBody SarActivityTypeDto dto) {
        return ResponseEntity.ok(sarFormService.updateActivityType(id, activityTypeId, dto));
    }

    @DeleteMapping("/{id}/activity-types/{activityTypeId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Remove a suspicious activity type from SAR")
    public ResponseEntity<Void> removeActivityType(
            @PathVariable UUID id,
            @PathVariable UUID activityTypeId) {
        sarFormService.removeActivityType(id, activityTypeId);
        return ResponseEntity.noContent().build();
    }

    // --- Branch Management ---

    @PostMapping("/{id}/branches")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Add branch where activity occurred (Part III)")
    public ResponseEntity<SarBranchDto> addBranch(
            @PathVariable UUID id,
            @RequestBody SarBranchDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sarFormService.addBranch(id, dto));
    }

    @PutMapping("/{id}/branches/{branchId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Update a branch record")
    public ResponseEntity<SarBranchDto> updateBranch(
            @PathVariable UUID id,
            @PathVariable UUID branchId,
            @RequestBody SarBranchDto dto) {
        return ResponseEntity.ok(sarFormService.updateBranch(id, branchId, dto));
    }

    @DeleteMapping("/{id}/branches/{branchId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Remove a branch from SAR")
    public ResponseEntity<Void> removeBranch(
            @PathVariable UUID id,
            @PathVariable UUID branchId) {
        sarFormService.removeBranch(id, branchId);
        return ResponseEntity.noContent().build();
    }

    // --- Audit History ---

    @GetMapping("/{id}/audit-history")
    @PreAuthorize("hasAnyRole('ANALYST', 'COMPLIANCE_OFFICER', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get audit history for a SAR report",
            description = "Returns chronological business audit events and field changes")
    public ResponseEntity<Page<AuditLogDto>> getAuditHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "performedAt") Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditHistory(id, pageable));
    }

    @GetMapping("/{id}/revision-history")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get Envers revision history for a SAR report",
            description = "Full field-level change history from Hibernate Envers")
    public ResponseEntity<List<RevisionHistoryDto>> getRevisionHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.getRevisionHistory(id));
    }
}
