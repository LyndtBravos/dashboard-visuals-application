package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.response.MonitorSummaryResponse;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;

import java.util.List;

public interface MonitorService {

    List<SiteMonitor> getAllSites(ServiceType serviceType);
    SiteMonitor getSite(Integer id);
    List<SiteMonitor> getFailingSites();
    List<SiteMonitor> getAlertingSites();
    SiteMonitor createSite(SiteMonitor site, String username);
    SiteMonitor updateSite(Integer id, SiteMonitor updatedSite, String username);
    void deleteSite(Integer id);
    void resetSiteFailureCount(Integer id);

    List<ApiMonitor> getAllApis(ServiceType serviceType);
    ApiMonitor getApi(Integer id);
    List<ApiMonitor> getFailingApis();
    List<ApiMonitor> getAlertingApis();
    ApiMonitor createApi(ApiMonitor api, String username);
    ApiMonitor updateApi(Integer id, ApiMonitor updatedApi, String username);
    void deleteApi(Integer id);
    void resetApiFailureCount(Integer id);

    List<MonitorSummaryResponse> getSummary();
}