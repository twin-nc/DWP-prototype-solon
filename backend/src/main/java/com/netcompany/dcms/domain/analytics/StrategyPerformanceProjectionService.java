package com.netcompany.dcms.domain.analytics;

import java.time.LocalDate;

/**
 * Service boundary for refreshing analytics strategy performance projections.
 */
public interface StrategyPerformanceProjectionService {

    void refreshForDate(LocalDate metricDate);
}

