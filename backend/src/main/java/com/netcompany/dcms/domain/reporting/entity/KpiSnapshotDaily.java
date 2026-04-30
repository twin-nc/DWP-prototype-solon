package com.netcompany.dcms.domain.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "kpi_snapshot_daily")
@IdClass(KpiSnapshotDailyId.class)
public class KpiSnapshotDaily {

    @Id
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Id
    @Column(name = "team_scope", nullable = false, length = 100)
    private String teamScope;

    @Id
    @Column(name = "segment_code", length = 100)
    private String segmentCode;

    @Column(name = "cure_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal cureRate;

    @Column(name = "contact_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal contactRate;

    @Column(name = "arrangement_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal arrangementRate;

    @Column(name = "breach_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal breachRate;

    @Column(name = "dca_recovery_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal dcaRecoveryRate;

    @Column(name = "write_off_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal writeOffRate;

    @Column(name = "accounts_in_scope", nullable = false)
    private Integer accountsInScope;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public String getTeamScope() {
        return teamScope;
    }

    public void setTeamScope(String teamScope) {
        this.teamScope = teamScope;
    }

    public String getSegmentCode() {
        return segmentCode;
    }

    public void setSegmentCode(String segmentCode) {
        this.segmentCode = segmentCode;
    }

    public BigDecimal getCureRate() {
        return cureRate;
    }

    public void setCureRate(BigDecimal cureRate) {
        this.cureRate = cureRate;
    }

    public BigDecimal getContactRate() {
        return contactRate;
    }

    public void setContactRate(BigDecimal contactRate) {
        this.contactRate = contactRate;
    }

    public BigDecimal getArrangementRate() {
        return arrangementRate;
    }

    public void setArrangementRate(BigDecimal arrangementRate) {
        this.arrangementRate = arrangementRate;
    }

    public BigDecimal getBreachRate() {
        return breachRate;
    }

    public void setBreachRate(BigDecimal breachRate) {
        this.breachRate = breachRate;
    }

    public BigDecimal getDcaRecoveryRate() {
        return dcaRecoveryRate;
    }

    public void setDcaRecoveryRate(BigDecimal dcaRecoveryRate) {
        this.dcaRecoveryRate = dcaRecoveryRate;
    }

    public BigDecimal getWriteOffRate() {
        return writeOffRate;
    }

    public void setWriteOffRate(BigDecimal writeOffRate) {
        this.writeOffRate = writeOffRate;
    }

    public Integer getAccountsInScope() {
        return accountsInScope;
    }

    public void setAccountsInScope(Integer accountsInScope) {
        this.accountsInScope = accountsInScope;
    }

    public OffsetDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(OffsetDateTime computedAt) {
        this.computedAt = computedAt;
    }
}

