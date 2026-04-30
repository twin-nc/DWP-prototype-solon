package com.netcompany.dcms.domain.reporting;

import java.time.LocalDate;

/**
 * Service boundary for KPI snapshot generation.
 */
public interface KpiSnapshotService {

    void generateDailySnapshot(LocalDate snapshotDate);
}

