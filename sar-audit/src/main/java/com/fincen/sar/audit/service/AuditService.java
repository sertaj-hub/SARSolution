package com.fincen.sar.audit.service;

import com.fincen.sar.audit.dto.AuditLogDto;
import com.fincen.sar.audit.dto.RevisionHistoryDto;
import com.fincen.sar.core.domain.AuditLog;
import com.fincen.sar.core.domain.SarReport;
import com.fincen.sar.core.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dual-track audit service:
 * 1. Business audit log (AuditLog entity) - for significant business events
 * 2. Hibernate Envers revision history - for full field-level change tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    /**
     * Record a business-level audit event for a SAR report action.
     */
    public void logSarEvent(UUID sarReportId, String action, String details) {
        logSarEvent(sarReportId, null, action, null, null, null, details);
    }

    /**
     * Record a field-level change audit event.
     */
    public void logFieldChange(UUID sarReportId, UUID entityId, String entityType,
                                String fieldName, String oldValue, String newValue, String reason) {
        AuditLog log = new AuditLog();
        log.setSarReportId(sarReportId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction("UPDATE");
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setPerformedBy(getCurrentUser());
        log.setIpAddress(getCurrentIpAddress());
        log.setPerformedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    /**
     * Record a status change event.
     */
    public void logStatusChange(UUID sarReportId, String fromStatus, String toStatus, String reason) {
        logSarEvent(sarReportId, sarReportId, "STATUS_CHANGE",
                "status", fromStatus, toStatus, reason);
    }

    /**
     * Record eFiling submission event.
     */
    public void logEFilingSubmission(UUID sarReportId, UUID batchId, String batchNumber) {
        logSarEvent(sarReportId, batchId, "EFILING_SUBMITTED",
                "batchNumber", null, batchNumber,
                "SAR submitted to FinCEN BSA E-Filing in batch: " + batchNumber);
    }

    /**
     * Record FinCEN acknowledgment event.
     */
    public void logFinCenAcknowledgment(UUID sarReportId, String bsaId, String status) {
        logSarEvent(sarReportId, sarReportId, "FINCEN_ACKNOWLEDGED",
                "bsaIdentifier", null, bsaId,
                "FinCEN acknowledgment received. Status: " + status + ", BSA ID: " + bsaId);
    }

    /**
     * Get audit history for a SAR report.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditHistory(UUID sarReportId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository
                .findBySarReportIdOrderByPerformedAtDesc(sarReportId, pageable);
        return page.map(this::toDto);
    }

    /**
     * Get full Envers revision history for a SAR report (field-level diffs).
     */
    @Transactional(readOnly = true)
    public List<RevisionHistoryDto> getRevisionHistory(UUID sarReportId) {
        try {
            AuditReader auditReader = AuditReaderFactory.get(entityManager);
            List<Number> revisions = auditReader.getRevisions(SarReport.class, sarReportId);

            List<RevisionHistoryDto> history = new ArrayList<>();
            for (Number revNum : revisions) {
                SarReport revState = auditReader.find(SarReport.class, sarReportId, revNum);
                if (revState != null) {
                    RevisionHistoryDto dto = new RevisionHistoryDto();
                    dto.setRevisionNumber(revNum.longValue());
                    dto.setSarReportId(sarReportId);
                    dto.setReportNumber(revState.getReportNumber());
                    dto.setStatus(revState.getStatus() != null ? revState.getStatus().name() : null);
                    dto.setNarrativeSnapshot(revState.getNarrative());
                    dto.setUpdatedBy(revState.getUpdatedBy());
                    dto.setUpdatedAt(revState.getUpdatedAt());
                    history.add(dto);
                }
            }
            return history;
        } catch (Exception e) {
            log.warn("Failed to fetch Envers revision history for SAR {}: {}", sarReportId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void logSarEvent(UUID sarReportId, UUID entityId, String action,
                              String fieldName, String oldValue, String newValue, String details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setSarReportId(sarReportId);
            auditLog.setEntityType("SarReport");
            auditLog.setEntityId(entityId != null ? entityId : sarReportId);
            auditLog.setAction(action);
            auditLog.setFieldName(fieldName);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setReason(details);
            auditLog.setPerformedBy(getCurrentUser());
            auditLog.setIpAddress(getCurrentIpAddress());
            auditLog.setPerformedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log for SAR {}: {}", sarReportId, e.getMessage());
        }
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "SYSTEM";
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String ip = attrs.getRequest().getHeader("X-Forwarded-For");
                return ip != null ? ip.split(",")[0].trim() : attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private AuditLogDto toDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setSarReportId(log.getSarReportId());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setAction(log.getAction());
        dto.setFieldName(log.getFieldName());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setPerformedBy(log.getPerformedBy());
        dto.setPerformedAt(log.getPerformedAt());
        dto.setIpAddress(log.getIpAddress());
        dto.setReason(log.getReason());
        return dto;
    }
}
