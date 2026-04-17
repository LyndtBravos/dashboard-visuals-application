package com.mediahost.dashboard.model.dto.response;

import com.mediahost.dashboard.model.enums.StatusColor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class QueryResultResponse {
    private Object value;
    private StatusColor status;
    private Double warningThreshold;
    private Double dangerThreshold;
    private String query;
    private LocalDateTime timestamp;
    private String errorMessage;
}