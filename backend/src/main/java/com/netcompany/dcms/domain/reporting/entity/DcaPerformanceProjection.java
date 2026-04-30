package com.netcompany.dcms.domain.reporting.entity;

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
@Table(name = "dca_performance_projection")
@IdClass(DcaPerformanceProjectionId.class)
public class DcaPerformanceProjection {

    @Id
    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Id
    @Column(name = "metric_month", nullable = false)
    private LocalDate metricMonth;

    @Column(name = "placements_count", nullable = false)
    private Integer placementsCount;

    @Column(name = "active_placements_count", nullable = false)
    private Integer activePlacementsCount;

    @Column(name = "recovered_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal recoveredAmount;

    @Column(name = "placed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal placedAmount;

    @Column(name = "recovery_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal recoveryRate;

    @Column(name = "commission_accrued", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionAccrued;

    @Column(name = "recall_count", nullable = false)
    private Integer recallCount;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

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

    public Integer getPlacementsCount() {
        return placementsCount;
    }

    public void setPlacementsCount(Integer placementsCount) {
        this.placementsCount = placementsCount;
    }

    public Integer getActivePlacementsCount() {
        return activePlacementsCount;
    }

    public void setActivePlacementsCount(Integer activePlacementsCount) {
        this.activePlacementsCount = activePlacementsCount;
    }

    public BigDecimal getRecoveredAmount() {
        return recoveredAmount;
    }

    public void setRecoveredAmount(BigDecimal recoveredAmount) {
        this.recoveredAmount = recoveredAmount;
    }

    public BigDecimal getPlacedAmount() {
        return placedAmount;
    }

    public void setPlacedAmount(BigDecimal placedAmount) {
        this.placedAmount = placedAmount;
    }

    public BigDecimal getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(BigDecimal recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public BigDecimal getCommissionAccrued() {
        return commissionAccrued;
    }

    public void setCommissionAccrued(BigDecimal commissionAccrued) {
        this.commissionAccrued = commissionAccrued;
    }

    public Integer getRecallCount() {
        return recallCount;
    }

    public void setRecallCount(Integer recallCount) {
        this.recallCount = recallCount;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(OffsetDateTime computedAt) {
        this.computedAt = computedAt;
    }
}
