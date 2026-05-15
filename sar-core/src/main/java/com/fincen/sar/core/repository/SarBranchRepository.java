package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.SarBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SarBranchRepository extends JpaRepository<SarBranch, UUID> {
}
