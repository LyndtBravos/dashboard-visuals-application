package com.mediahost.dashboard.service.checker;

import com.mediahost.dashboard.model.enums.MonitorStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckResult {
    private MonitorStatus status;
    private Integer responseTimeMs;
    private Integer statusCode;
    private String responsePreview;
    private String errorMessage;
    private Integer responseSizeBytes;
    private String responseBody;
}