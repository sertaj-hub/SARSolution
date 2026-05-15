package com.fincen.sar.core.repository;

import com.fincen.sar.core.domain.SarActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SarActivityTypeRepository extends JpaRepository<SarActivityType, UUID> {
}
