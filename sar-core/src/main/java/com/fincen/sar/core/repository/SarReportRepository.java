package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.SarReport;
import com.fincen.sar.core.enums.SarStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SarReportRepository extends JpaRepository<SarReport, UUID>,
        JpaSpecificationExecutor<SarReport> {

    Optional<SarReport> findByReportNumber(String reportNumber);

    Optional<SarReport> findByBsaIdentifier(String bsaIdentifier);

    List<SarReport> findByCaseId(String caseId);

    Page<SarReport> findByStatus(SarStatus status, Pageable pageable);

    List<SarReport> findByStatusAndFilingDateBefore(SarStatus status, LocalDate date);

    @Query("SELECT r FROM SarReport r WHERE r.status = 'APPROVED' AND r.batchId IS NULL")
    List<SarReport> findApprovedReportsNotInBatch();

    @Query("SELECT r FROM SarReport r WHERE r.batchId = :batchId")
    List<SarReport> findByBatchId(@Param("batchId") UUID batchId);

    boolean existsByReportNumber(String reportNumber);

    @Query("SELECT COUNT(r) FROM SarReport r WHERE r.status = :status")
    long countByStatus(@Param("status") SarStatus status);
}
