package com.mediahost.dashboard.model.dto.response;

import lombok.Builder;
import lombok.Data;
import com.mediahost.dashboard.model.enums.*;
import java.time.LocalDateTime;

@Data
@Builder
public class AlertDetailResponse {
    private Integer id;
    private MonitorType monitorType;
    private Integer monitorId;
    private String name;
    private Severity severity;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime lastOccurrence;
    private Integer occurrenceCount;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;

    private String url;           // For Site/API monitors
    private String method;        // For API monitors
    private String host;          // For Server monitors
    private Integer port;         // For Server monitors
    private String protocol;      // For Server monitors
    private String queryText;     // For Dashboard visuals
    private String graphType;     // For Dashboard visuals
    private String serviceType;
    private Integer expectedStatusCode;
    private Integer currentFailureCount;
    private Integer retryCount;
    private String lastCheckMessage;
    private LocalDateTime lastCheckTime;
}