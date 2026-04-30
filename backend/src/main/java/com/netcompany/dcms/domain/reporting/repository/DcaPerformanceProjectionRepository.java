package com.netcompany.dcms.domain.reporting.repository;

import com.netcompany.dcms.domain.reporting.entity.DcaPerformanceProjection;
import com.netcompany.dcms.domain.reporting.entity.DcaPerformanceProjectionId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DcaPerformanceProjectionRepository
    extends JpaRepository<DcaPerformanceProjection, DcaPerformanceProjectionId> {

    List<DcaPerformanceProjection> findByMetricMonth(LocalDate metricMonth);
}

