package com.mediahost.dashboard.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class QueryDataSetResponse {
    private Boolean success;
    private List<Map<String, Object>> data;
    private List<String> columns;
    private Integer rowCount;
    private String query;
    private LocalDateTime timestamp;
    private String errorMessage;
}