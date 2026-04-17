package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.request.DashboardCreateRequest;
import com.mediahost.dashboard.model.dto.request.DashboardUpdateRequest;
import com.mediahost.dashboard.model.dto.response.DashboardResponse;
import com.mediahost.dashboard.model.dto.response.ServiceSummaryResponse;
import com.mediahost.dashboard.model.entity.DashboardConfig;
import com.mediahost.dashboard.model.enums.ServiceType;

import java.util.List;

public interface DashboardService {
    List<DashboardResponse> getOrderedDashboardsByService(ServiceType serviceType);
    List<DashboardResponse> getAllDashboards(ServiceType serviceType);
    List<DashboardResponse> getDashboardsByService(ServiceType serviceType);
    DashboardResponse getDashboard(Integer id);
    DashboardResponse createDashboard(DashboardCreateRequest request, String username);
    DashboardResponse updateDashboard(Integer id, DashboardUpdateRequest request, String username);
    void deleteDashboard(Integer id);
    void updateStatus(Integer id, boolean active);
    ServiceSummaryResponse getServiceSummary(ServiceType serviceType);
    List<ServiceSummaryResponse> getAllServicesSummary();
}