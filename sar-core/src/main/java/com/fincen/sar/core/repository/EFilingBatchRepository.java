package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.EFilingBatch;
import com.fincen.sar.core.enums.FilingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EFilingBatchRepository extends JpaRepository<EFilingBatch, UUID> {
    Optional<EFilingBatch> findByBatchNumber(String batchNumber);
    Optional<EFilingBatch> findByFincenTrackingId(String trackingId);
    List<EFilingBatch> findByStatus(FilingStatus status);
    List<EFilingBatch> findByStatusIn(List<FilingStatus> statuses);
}
