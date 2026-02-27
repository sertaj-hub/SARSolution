package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.SarSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SarSubjectRepository extends JpaRepository<SarSubject, UUID> {
    List<SarSubject> findBySarReportId(UUID sarReportId);
    List<SarSubject> findByCaseSubjectId(String caseSubjectId);
}
