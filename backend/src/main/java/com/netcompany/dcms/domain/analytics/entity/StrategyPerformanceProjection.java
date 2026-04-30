package com.netcompany.dcms.domain.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "strategy_performance_projection")
@IdClass(StrategyPerformanceProjectionId.class)
public class StrategyPerformanceProjection {

    @Id
    @Column(name = "strategy_test_id", nullable = false)
    private UUID strategyTestId;

    @Id
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Id
    @Column(name = "variant", nullable = false, length = 20)
    private String variant;

    @Id
    @Column(name = "vulnerability_override", nullable = false)
    private Boolean vulnerabilityOverride;

    @Column(name = "accounts_count", nullable = false)
    private Integer accountsCount;

    @Column(name = "payments_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal paymentsTotal;

    @Column(name = "arrangements_count", nullable = false)
    private Integer arrangementsCount;

    @Column(name = "breaches_count", nullable = false)
    private Integer breachesCount;

    @Column(name = "complaints_count", nullable = false)
    private Integer complaintsCount;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

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

    public Integer getAccountsCount() {
        return accountsCount;
    }

    public void setAccountsCount(Integer accountsCount) {
        this.accountsCount = accountsCount;
    }

    public BigDecimal getPaymentsTotal() {
        return paymentsTotal;
    }

    public void setPaymentsTotal(BigDecimal paymentsTotal) {
        this.paymentsTotal = paymentsTotal;
    }

    public Integer getArrangementsCount() {
        return arrangementsCount;
    }

    public void setArrangementsCount(Integer arrangementsCount) {
        this.arrangementsCount = arrangementsCount;
    }

    public Integer getBreachesCount() {
        return breachesCount;
    }

    public void setBreachesCount(Integer breachesCount) {
        this.breachesCount = breachesCount;
    }

    public Integer getComplaintsCount() {
        return complaintsCount;
    }

    public void setComplaintsCount(Integer complaintsCount) {
        this.complaintsCount = complaintsCount;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(OffsetDateTime computedAt) {
        this.computedAt = computedAt;
    }
}
