package com.netcompany.dcms.domain.analytics.repository;

import com.netcompany.dcms.domain.analytics.entity.StrategyPerformanceProjection;
import com.netcompany.dcms.domain.analytics.entity.StrategyPerformanceProjectionId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrategyPerformanceProjectionRepository
    extends JpaRepository<StrategyPerformanceProjection, StrategyPerformanceProjectionId> {

    List<StrategyPerformanceProjection> findByMetricDate(LocalDate metricDate);
}

