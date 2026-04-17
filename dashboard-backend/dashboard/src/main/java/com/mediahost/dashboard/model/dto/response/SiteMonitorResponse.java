package com.mediahost.dashboard.model.dto.response;

import com.mediahost.dashboard.model.enums.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class SiteMonitorResponse {
    private Integer id;
    private String name;
    private String description;
    private String url;
    private String expectedPhrase;
    private Boolean expectedPhraseMissing;
    private Integer retryCount;
    private Integer retryIntervalSeconds;
    private Integer checkIntervalMinutes;
    private Integer timeoutSeconds;
    private Boolean followRedirects;
    private Integer expectedStatusCode;
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
    private Integer currentFailureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}