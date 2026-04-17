package com.mediahost.dashboard.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitorSummaryResponse {
    private String monitorType;
    private long total;
    private long healthy;
    private long failing;
    private long alerting;
}