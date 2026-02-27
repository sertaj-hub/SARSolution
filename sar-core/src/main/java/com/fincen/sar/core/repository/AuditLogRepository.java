package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findBySarReportIdOrderByPerformedAtDesc(UUID sarReportId, Pageable pageable);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    List<AuditLog> findByPerformedByAndPerformedAtBetween(String user, LocalDateTime from, LocalDateTime to);
}
