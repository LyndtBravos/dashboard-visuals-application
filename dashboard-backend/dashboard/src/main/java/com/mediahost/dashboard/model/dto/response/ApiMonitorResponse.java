package com.mediahost.dashboard.model.dto.response;

import com.mediahost.dashboard.model.enums.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ApiMonitorResponse {
    private Integer id;
    private String name;
    private String description;
    private String url;
    private HttpMethod method;
    private String requestHeadersJson;
    private String requestBody;
    private String requestContentType;
    private Integer expectedStatusCode;
    private Integer expectedResponseTimeMs;
    private Integer expectedResponseSizeBytes;
    private String expectedResponseContains;
    private String expectedJsonPath;
    private String expectedValue;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Integer retryIntervalSeconds;
    private Integer checkIntervalMinutes;
    private ServiceType serviceType;
    private Severity severity;
    private Boolean businessHoursOnly;
    private LocalTime businessHoursStart;
    private LocalTime businessHoursEnd;
    private String businessDays;
    private Boolean isActive;
    private Boolean alert;
    private String alertEmail;
    private LocalDateTime lastCheckTime;
    private MonitorStatus lastCheckStatus;
    private String lastCheckMessage;
    private Integer lastResponseTimeMs;
    private Integer lastResponseSizeBytes;
    private Integer currentFailureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}