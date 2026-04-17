package com.mediahost.dashboard.model.dto.response;

import lombok.Builder;
import lombok.Data;
import com.mediahost.dashboard.model.enums.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ServerMonitorResponse {
    private Integer id;
    private String name;
    private String description;
    private String host;
    private Integer port;
    private ServerProtocol protocol;
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
    private Integer responseTimeMs;
    private Integer currentFailureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}