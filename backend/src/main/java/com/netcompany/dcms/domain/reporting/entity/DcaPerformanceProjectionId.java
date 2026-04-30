package com.netcompany.dcms.domain.reporting.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class DcaPerformanceProjectionId implements Serializable {

    private UUID agencyId;
    private LocalDate metricMonth;

    public DcaPerformanceProjectionId() {
    }

    public DcaPerformanceProjectionId(UUID agencyId, LocalDate metricMonth) {
        this.agencyId = agencyId;
        this.metricMonth = metricMonth;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(UUID agencyId) {
        this.agencyId = agencyId;
    }

    public LocalDate getMetricMonth() {
        return metricMonth;
    }

    public void setMetricMonth(LocalDate metricMonth) {
        this.metricMonth = metricMonth;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        DcaPerformanceProjectionId that = (DcaPerformanceProjectionId) other;
        return Objects.equals(agencyId, that.agencyId)
            && Objects.equals(metricMonth, that.metricMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId, metricMonth);
    }
}
