package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.response.*;
import java.util.List;

public interface MonitorSummaryService {
    List<MonitorSummaryResponse> getSummary();
    List<MonitorAlertViewResponse> getActiveAlerts();
    void acknowledgeAlert(Integer alertId);
}