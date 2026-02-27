package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.SarTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SarTransactionRepository extends JpaRepository<SarTransaction, UUID> {
    List<SarTransaction> findBySarReportId(UUID sarReportId);
    List<SarTransaction> findByCaseTransactionId(String caseTransactionId);
    List<SarTransaction> findByAlertId(String alertId);
}
