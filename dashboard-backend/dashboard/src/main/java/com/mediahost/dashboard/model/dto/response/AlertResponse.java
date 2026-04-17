package com.mediahost.dashboard.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AlertResponse {
    private Integer id;
    private Integer dashboardId;
    private String dashboardName;
    private String serviceType;
    private Object currentValue;
    private Double thresholdDanger;
    private String status;
    private LocalDateTime startedAt;
    private Boolean acknowledged;
    private String alertEmail;
    private Integer acknowledgedBy;
    private LocalDateTime acknowledgedAt;
}