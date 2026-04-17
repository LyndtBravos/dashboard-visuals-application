package com.mediahost.dashboard.model.entity;

import com.mediahost.dashboard.model.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "api_monitors")
@Data
public class ApiMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 200)
    @Column(length = 200, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(http|https)://.*$", message = "Invalid URL format")
    @Column(length = 500, nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private HttpMethod method = HttpMethod.GET;

    @Column(name = "request_headers_json", columnDefinition = "TEXT")
    private String requestHeadersJson;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "request_content_type", length = 100)
    private String requestContentType = "application/json";

    @Column(name = "expected_status_code")
    @Min(100) @Max(599)
    private Integer expectedStatusCode = 200;

    @Column(name = "expected_response_time_ms")
    @Min(0)
    private Integer expectedResponseTimeMs;

    @Column(name = "expected_response_size_bytes")
    @Min(0)
    private Integer expectedResponseSizeBytes;

    @Column(name = "expected_response_contains")
    private String expectedResponseContains;

    @Column(name = "expected_json_path")
    private String expectedJsonPath;

    @Column(name = "expected_value")
    private String expectedValue;

    @Column(name = "timeout_seconds")
    @Min(1) @Max(120)
    private Integer timeoutSeconds = 30;

    @Column(name = "retry_count")
    @Min(0) @Max(10)
    private Integer retryCount = 3;

    @Column(name = "retry_interval_seconds")
    @Min(5) @Max(3600)
    private Integer retryIntervalSeconds = 60;

    @Column(name = "check_interval_minutes")
    @Min(1) @Max(1440)
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
    private LocalTime businessHoursEnd = LocalTime.of(20, 0);

    @Column(name = "business_days", length = 50)
    private String businessDays = "1,2,3,4,5";

    @Column(name = "is_active")
    private Boolean isActive = true;

    private Boolean alert = true;

    @Column(name = "alert_email")
    @Email
    private String alertEmail;

    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_check_status")
    private MonitorStatus lastCheckStatus;

    @Column(name = "last_check_message", columnDefinition = "TEXT")
    private String lastCheckMessage;

    @Column(name = "last_response_time_ms")
    private Integer lastResponseTimeMs;

    @Column(name = "last_response_size_bytes")
    private Integer lastResponseSizeBytes;

    @Column(name = "last_response_body", columnDefinition = "TEXT")
    private String lastResponseBody;

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