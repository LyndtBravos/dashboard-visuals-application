package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.response.QueryDataSetResponse;
import com.mediahost.dashboard.model.enums.StatusColor;
import java.util.Map;

public interface QueryService {
    Object executeQuery(String query);
    StatusColor evaluateThreshold(Object value, Double warningThreshold, Double dangerThreshold);
    Map<String, Object> executeQueryWithMetadata(String query);
}