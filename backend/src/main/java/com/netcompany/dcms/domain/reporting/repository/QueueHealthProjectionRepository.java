package com.netcompany.dcms.domain.reporting.repository;

import com.netcompany.dcms.domain.reporting.entity.QueueHealthProjection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueHealthProjectionRepository extends JpaRepository<QueueHealthProjection, UUID> {
}

