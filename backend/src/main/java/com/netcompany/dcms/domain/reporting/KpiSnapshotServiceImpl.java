package com.netcompany.dcms.domain.reporting;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KpiSnapshotServiceImpl implements KpiSnapshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KpiSnapshotServiceImpl.class);

    @Override
    public void generateDailySnapshot(LocalDate snapshotDate) {
        LOGGER.info("KPI daily snapshot generation requested for {}", snapshotDate);
    }
}

