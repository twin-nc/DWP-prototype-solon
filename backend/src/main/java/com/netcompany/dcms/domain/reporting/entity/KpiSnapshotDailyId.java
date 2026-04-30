package com.netcompany.dcms.domain.reporting.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class KpiSnapshotDailyId implements Serializable {

    private LocalDate snapshotDate;
    private String teamScope;
    private String segmentCode;

    public KpiSnapshotDailyId() {
    }

    public KpiSnapshotDailyId(LocalDate snapshotDate, String teamScope, String segmentCode) {
        this.snapshotDate = snapshotDate;
        this.teamScope = teamScope;
        this.segmentCode = segmentCode;
    }

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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        KpiSnapshotDailyId that = (KpiSnapshotDailyId) other;
        return Objects.equals(snapshotDate, that.snapshotDate)
            && Objects.equals(teamScope, that.teamScope)
            && Objects.equals(segmentCode, that.segmentCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotDate, teamScope, segmentCode);
    }
}

