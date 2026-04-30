package com.netcompany.dcms.domain.reporting.repository;

import com.netcompany.dcms.domain.reporting.entity.KpiSnapshotDaily;
import com.netcompany.dcms.domain.reporting.entity.KpiSnapshotDailyId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiSnapshotDailyRepository extends JpaRepository<KpiSnapshotDaily, KpiSnapshotDailyId> {

    List<KpiSnapshotDaily> findBySnapshotDate(LocalDate snapshotDate);
}

