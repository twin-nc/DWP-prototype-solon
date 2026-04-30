package com.netcompany.dcms.domain.analytics;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StrategyPerformanceProjectionServiceImpl implements StrategyPerformanceProjectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPerformanceProjectionServiceImpl.class);

    @Override
    public void refreshForDate(LocalDate metricDate) {
        LOGGER.info("Strategy performance projection refresh requested for {}", metricDate);
    }
}

