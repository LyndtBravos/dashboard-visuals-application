package com.mediahost.dashboard.model.dto.response;

import com.mediahost.dashboard.model.enums.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MonitorAlertViewResponse {
    private Integer id;
    private MonitorType monitorType;
    private Integer monitorId;
    private String name;
    private Severity severity;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime lastOccurrence;
    private int occurrenceCount;
    private boolean acknowledged;
    private LocalDateTime resolvedAt;
    private int acknowledgedBy;
}