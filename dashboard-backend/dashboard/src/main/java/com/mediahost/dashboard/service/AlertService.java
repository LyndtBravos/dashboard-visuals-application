package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.response.AlertDetailResponse;
import com.mediahost.dashboard.model.dto.response.MonitorAlertViewResponse;
import com.mediahost.dashboard.model.enums.AlertStatus;

import java.util.List;
import java.util.Map;

public interface AlertService {
    List<MonitorAlertViewResponse> getAllAlerts(Boolean acknowledged, AlertStatus status);
    List<MonitorAlertViewResponse> getActiveAlerts();
    MonitorAlertViewResponse getAlert(Integer id);
    AlertDetailResponse getAlertDetail(Integer id);
    void acknowledgeAlert(Integer alertId, String username);
    void resolveAlert(Integer alertId);
    Map<String, Long> getAlertCount();
}
