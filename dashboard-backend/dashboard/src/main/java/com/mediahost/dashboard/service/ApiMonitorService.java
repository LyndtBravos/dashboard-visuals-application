package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.request.ApiMonitorRequest;
import com.mediahost.dashboard.model.dto.response.ApiMonitorResponse;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;
import java.util.List;

public interface ApiMonitorService {
    List<ApiMonitorResponse> getAllApis(ServiceType serviceType);
    ApiMonitorResponse getApiById(Integer id);
    List<ApiMonitorResponse> getFailingApis();
    List<ApiMonitorResponse> getAlertingApis();
    ApiMonitorResponse createApi(ApiMonitorRequest request, String username);
    ApiMonitorResponse updateApi(Integer id, ApiMonitorRequest request, String username);
    void deleteApi(Integer id);
    void resetFailureCount(Integer id);
    ApiMonitorResponse recheckApi(Integer id);
}