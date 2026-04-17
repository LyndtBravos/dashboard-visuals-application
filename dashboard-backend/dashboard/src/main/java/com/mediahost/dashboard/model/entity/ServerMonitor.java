package com.mediahost.dashboard.model.entity;

import com.mediahost.dashboard.model.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "server_monitors")
@Data
public class ServerMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255, nullable = false)
    private String host;

    private Integer port;

    @Enumerated(EnumType.STRING)
    private ServerProtocol protocol = ServerProtocol.icmp;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 5;

    @Column(name = "retry_count")
    private Integer retryCount = 2;

    @Column(name = "retry_interval_seconds")
    private Integer retryIntervalSeconds = 30;

    @Column(name = "check_interval_minutes")
    private Integer checkIntervalMinutes = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.medium;

    @Column(name = "business_hours_only")
    private Boolean businessHoursOnly = false;

    @Column(name = "business_hours_start")
    private LocalTime businessHoursStart = LocalTime.of(6, 0);

    @Column(name = "business_hours_end")
    private LocalTime businessHoursEnd = LocalTime.of(21, 0);

    @Column(name = "business_days", length = 50)
    private String businessDays = "1,2,3,4,5,6,7";

    @Column(name = "is_active")
    private Boolean isActive = true;

    private Boolean alert = true;

    @Column(name = "alert_email")
    private String alertEmail;

    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_check_status")
    private MonitorStatus lastCheckStatus;

    @Column(name = "last_check_message", columnDefinition = "TEXT")
    private String lastCheckMessage;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "current_failure_count")
    private Integer currentFailureCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}