package com.mediahost.dashboard.model.entity;

import com.mediahost.dashboard.model.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitor_history")
@Data
public class MonitorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitor_type", nullable = false)
    private MonitorType monitorType;

    @Column(name = "monitor_id", nullable = false)
    private Integer monitorId;

    @Column(name = "check_time", nullable = false)
    private LocalDateTime checkTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitorStatus status = MonitorStatus.error;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_preview", columnDefinition = "TEXT")
    private String responsePreview;

    @Column(name = "error_message", columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(name = "failure_count_at_time")
    private Integer failureCountAtTime;

    @Column(name = "alert_created")
    private Boolean alertCreated = false;
}