package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.SarAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SarAccountRepository extends JpaRepository<SarAccount, UUID> {
    List<SarAccount> findBySarReportId(UUID sarReportId);
    List<SarAccount> findByCaseAccountId(String caseAccountId);
}
