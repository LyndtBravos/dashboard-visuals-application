package com.mediahost.dashboard.model.dto.response;

import com.mediahost.dashboard.model.enums.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DashboardResponse {
    private Integer id;
    private String name;
    private String description;
    private ServiceType serviceType;
    private GraphType graphType;
    private String queryText;
    private Double thresholdWarning;
    private Double thresholdDanger;
    private String alertEmail;
    private Boolean alert;
    private Width width;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer updatedBy;
    private Integer flowOrder;

    private String xAxisColumn;
    private String yAxisColumn;
    private String timeInterval;
    private String seriesColumns;
    private AggregationType aggregationType;

    // Dynamic fields (populated when query is executed)
    private Object currentValue;
    private StatusColor currentStatus;
    private LocalDateTime lastChecked;
}