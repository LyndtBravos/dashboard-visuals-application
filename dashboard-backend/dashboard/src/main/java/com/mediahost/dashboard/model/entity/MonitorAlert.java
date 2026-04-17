package com.mediahost.dashboard.model.entity;

import com.mediahost.dashboard.model.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitor_alerts")
@Data
public class MonitorAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitor_type", nullable = false)
    private MonitorType monitorType;

    @Column(name = "monitor_id", nullable = false)
    private Integer monitorId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_occurrence", nullable = false)
    private LocalDateTime lastOccurrence;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(name = "failure_reason", columnDefinition = "TEXT", nullable = false)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status")
    private AlertStatus currentStatus = AlertStatus.failing;

    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private Integer acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}