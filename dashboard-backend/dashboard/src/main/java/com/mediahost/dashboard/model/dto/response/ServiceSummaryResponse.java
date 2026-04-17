package com.mediahost.dashboard.model.dto.response;

import com.mediahost.dashboard.model.enums.ServiceType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ServiceSummaryResponse {
    private ServiceType serviceType;
    private Integer totalVisuals;
    private Map<String, Integer> statusCounts;
    private LocalDateTime timestamp;
}