package com.netcompany.dcms.domain.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_health_projection")
public class QueueHealthProjection {

    @Id
    @Column(name = "queue_id", nullable = false)
    private UUID queueId;

    @Column(name = "as_of_ts", nullable = false)
    private OffsetDateTime asOfTs;

    @Column(name = "queue_depth", nullable = false)
    private Integer queueDepth;

    @Column(name = "unassigned_count", nullable = false)
    private Integer unassignedCount;

    @Column(name = "sla_warning_count", nullable = false)
    private Integer slaWarningCount;

    @Column(name = "sla_breach_count", nullable = false)
    private Integer slaBreachCount;

    @Column(name = "avg_age_minutes", nullable = false)
    private Integer avgAgeMinutes;

    @Column(name = "team_scope", length = 100)
    private String teamScope;

    public UUID getQueueId() {
        return queueId;
    }

    public void setQueueId(UUID queueId) {
        this.queueId = queueId;
    }

    public OffsetDateTime getAsOfTs() {
        return asOfTs;
    }

    public void setAsOfTs(OffsetDateTime asOfTs) {
        this.asOfTs = asOfTs;
    }

    public Integer getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(Integer queueDepth) {
        this.queueDepth = queueDepth;
    }

    public Integer getUnassignedCount() {
        return unassignedCount;
    }

    public void setUnassignedCount(Integer unassignedCount) {
        this.unassignedCount = unassignedCount;
    }

    public Integer getSlaWarningCount() {
        return slaWarningCount;
    }

    public void setSlaWarningCount(Integer slaWarningCount) {
        this.slaWarningCount = slaWarningCount;
    }

    public Integer getSlaBreachCount() {
        return slaBreachCount;
    }

    public void setSlaBreachCount(Integer slaBreachCount) {
        this.slaBreachCount = slaBreachCount;
    }

    public Integer getAvgAgeMinutes() {
        return avgAgeMinutes;
    }

    public void setAvgAgeMinutes(Integer avgAgeMinutes) {
        this.avgAgeMinutes = avgAgeMinutes;
    }

    public String getTeamScope() {
        return teamScope;
    }

    public void setTeamScope(String teamScope) {
        this.teamScope = teamScope;
    }
}

