package com.netcompany.dcms.domain.analytics.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class StrategyPerformanceProjectionId implements Serializable {

    private UUID strategyTestId;
    private LocalDate metricDate;
    private String variant;
    private Boolean vulnerabilityOverride;

    public StrategyPerformanceProjectionId() {
    }

    public StrategyPerformanceProjectionId(
        UUID strategyTestId,
        LocalDate metricDate,
        String variant,
        Boolean vulnerabilityOverride
    ) {
        this.strategyTestId = strategyTestId;
        this.metricDate = metricDate;
        this.variant = variant;
        this.vulnerabilityOverride = vulnerabilityOverride;
    }

    public UUID getStrategyTestId() {
        return strategyTestId;
    }

    public void setStrategyTestId(UUID strategyTestId) {
        this.strategyTestId = strategyTestId;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.metricDate = metricDate;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public Boolean getVulnerabilityOverride() {
        return vulnerabilityOverride;
    }

    public void setVulnerabilityOverride(Boolean vulnerabilityOverride) {
        this.vulnerabilityOverride = vulnerabilityOverride;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        StrategyPerformanceProjectionId that = (StrategyPerformanceProjectionId) other;
        return Objects.equals(strategyTestId, that.strategyTestId)
            && Objects.equals(metricDate, that.metricDate)
            && Objects.equals(variant, that.variant)
            && Objects.equals(vulnerabilityOverride, that.vulnerabilityOverride);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategyTestId, metricDate, variant, vulnerabilityOverride);
    }
}
