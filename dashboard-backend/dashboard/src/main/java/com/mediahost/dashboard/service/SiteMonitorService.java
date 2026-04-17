package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.request.SiteMonitorRequest;
import com.mediahost.dashboard.model.dto.response.SiteMonitorResponse;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;
import java.util.List;

public interface SiteMonitorService {
    List<SiteMonitorResponse> getAllSites(ServiceType serviceType);
    SiteMonitorResponse getSiteById(Integer id);
    List<SiteMonitorResponse> getFailingSites();
    List<SiteMonitorResponse> getAlertingSites();
    SiteMonitorResponse createSite(SiteMonitorRequest request, String username);
    SiteMonitorResponse updateSite(Integer id, SiteMonitorRequest request, String username);
    SiteMonitorResponse recheckSite(Integer id);
    void deleteSite(Integer id);
    void resetFailureCount(Integer id);
}