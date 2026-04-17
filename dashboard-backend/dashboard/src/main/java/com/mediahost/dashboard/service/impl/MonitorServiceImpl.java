package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.dto.response.MonitorSummaryResponse;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.ServiceType;
import com.mediahost.dashboard.repository.*;
import com.mediahost.dashboard.service.MonitorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorServiceImpl implements MonitorService {

    private final SiteMonitorRepository siteMonitorRepository;
    private final ApiMonitorRepository apiMonitorRepository;
    private final UserRepository userRepository;

    @Override
    public List<SiteMonitor> getAllSites(ServiceType serviceType) {
        if (serviceType != null)
            return siteMonitorRepository.findByServiceTypeAndIsActiveTrue(serviceType);

        return siteMonitorRepository.findByIsActiveTrue();
    }

    @Override
    public SiteMonitor getSite(Integer id) {
        return siteMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site monitor not found: " + id));
    }

    @Override
    public List<SiteMonitor> getFailingSites() {
        return siteMonitorRepository.findFailingMonitors();
    }

    @Override
    public List<SiteMonitor> getAlertingSites() {
        return siteMonitorRepository.findAlertingMonitors();
    }

    @Override
    @Transactional
    public SiteMonitor createSite(SiteMonitor site, String username) {
        Integer userId = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();

        site.setUpdatedBy(userId);
        return siteMonitorRepository.save(site);
    }

    @Override
    @Transactional
    public SiteMonitor updateSite(Integer id, SiteMonitor updatedSite, String username) {
        SiteMonitor existing = getSite(id);

        Integer userId = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();

        existing.setName(updatedSite.getName());
        existing.setDescription(updatedSite.getDescription());
        existing.setUrl(updatedSite.getUrl());
        existing.setExpectedPhrase(updatedSite.getExpectedPhrase());
        existing.setExpectedPhraseMissing(updatedSite.getExpectedPhraseMissing());
        existing.setRetryCount(updatedSite.getRetryCount());
        existing.setRetryIntervalSeconds(updatedSite.getRetryIntervalSeconds());
        existing.setCheckIntervalMinutes(updatedSite.getCheckIntervalMinutes());
        existing.setTimeoutSeconds(updatedSite.getTimeoutSeconds());
        existing.setFollowRedirects(updatedSite.getFollowRedirects());
        existing.setExpectedStatusCode(updatedSite.getExpectedStatusCode());
        existing.setServiceType(updatedSite.getServiceType());
        existing.setSeverity(updatedSite.getSeverity());
        existing.setBusinessHoursOnly(updatedSite.getBusinessHoursOnly());
        existing.setBusinessHoursStart(updatedSite.getBusinessHoursStart());
        existing.setBusinessHoursEnd(updatedSite.getBusinessHoursEnd());
        existing.setBusinessDays(updatedSite.getBusinessDays());
        existing.setIsActive(updatedSite.getIsActive());
        existing.setAlert(updatedSite.getAlert());
        existing.setAlertEmail(updatedSite.getAlertEmail());
        existing.setUpdatedBy(userId);

        return siteMonitorRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteSite(Integer id) {
        SiteMonitor site = getSite(id);
        site.setIsActive(false);
        siteMonitorRepository.save(site);
        log.info("Site monitor {} deactivated", id);
    }

    @Override
    @Transactional
    public void resetSiteFailureCount(Integer id) {
        SiteMonitor site = getSite(id);
        site.setCurrentFailureCount(0);
        siteMonitorRepository.save(site);
        log.info("Site monitor {} failure count reset", id);
    }

    @Override
    public List<ApiMonitor> getAllApis(ServiceType serviceType) {
        if (serviceType != null) {
            return apiMonitorRepository.findByServiceTypeAndIsActiveTrue(serviceType);
        }
        return apiMonitorRepository.findByIsActiveTrue();
    }

    @Override
    public ApiMonitor getApi(Integer id) {
        return apiMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API monitor not found: " + id));
    }

    @Override
    public List<ApiMonitor> getFailingApis() {
        return apiMonitorRepository.findFailingMonitors();
    }

    @Override
    public List<ApiMonitor> getAlertingApis() {
        return apiMonitorRepository.findAlertingMonitors();
    }

    @Override
    @Transactional
    public ApiMonitor createApi(ApiMonitor api, String username) {
        Integer userId = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();

        api.setUpdatedBy(userId);
        return apiMonitorRepository.save(api);
    }

    @Override
    @Transactional
    public ApiMonitor updateApi(Integer id, ApiMonitor updatedApi, String username) {
        ApiMonitor existing = getApi(id);

        Integer userId = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();

        existing.setName(updatedApi.getName());
        existing.setDescription(updatedApi.getDescription());
        existing.setUrl(updatedApi.getUrl());
        existing.setMethod(updatedApi.getMethod());
        existing.setRequestHeadersJson(updatedApi.getRequestHeadersJson());
        existing.setRequestBody(updatedApi.getRequestBody());
        existing.setRequestContentType(updatedApi.getRequestContentType());
        existing.setExpectedStatusCode(updatedApi.getExpectedStatusCode());
        existing.setExpectedResponseTimeMs(updatedApi.getExpectedResponseTimeMs());
        existing.setExpectedResponseSizeBytes(updatedApi.getExpectedResponseSizeBytes());
        existing.setExpectedResponseContains(updatedApi.getExpectedResponseContains());
        existing.setExpectedJsonPath(updatedApi.getExpectedJsonPath());
        existing.setExpectedValue(updatedApi.getExpectedValue());
        existing.setTimeoutSeconds(updatedApi.getTimeoutSeconds());
        existing.setRetryCount(updatedApi.getRetryCount());
        existing.setRetryIntervalSeconds(updatedApi.getRetryIntervalSeconds());
        existing.setCheckIntervalMinutes(updatedApi.getCheckIntervalMinutes());
        existing.setServiceType(updatedApi.getServiceType());
        existing.setSeverity(updatedApi.getSeverity());
        existing.setBusinessHoursOnly(updatedApi.getBusinessHoursOnly());
        existing.setBusinessHoursStart(updatedApi.getBusinessHoursStart());
        existing.setBusinessHoursEnd(updatedApi.getBusinessHoursEnd());
        existing.setBusinessDays(updatedApi.getBusinessDays());
        existing.setIsActive(updatedApi.getIsActive());
        existing.setAlert(updatedApi.getAlert());
        existing.setAlertEmail(updatedApi.getAlertEmail());
        existing.setUpdatedBy(userId);

        return apiMonitorRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteApi(Integer id) {
        ApiMonitor api = getApi(id);
        api.setIsActive(false);
        apiMonitorRepository.save(api);
        log.info("API monitor {} deactivated", id);
    }

    @Override
    @Transactional
    public void resetApiFailureCount(Integer id) {
        ApiMonitor api = getApi(id);
        api.setCurrentFailureCount(0);
        apiMonitorRepository.save(api);
        log.info("API monitor {} failure count reset", id);
    }

    @Override
    public List<MonitorSummaryResponse> getSummary() {
        long siteTotal = siteMonitorRepository.count();
        long siteHealthy = siteMonitorRepository.countHealthy();
        long siteFailing = siteMonitorRepository.countFailing();
        long siteAlerting = siteMonitorRepository.findAlertingMonitors().size();

        long apiTotal = apiMonitorRepository.count();
        long apiHealthy = apiMonitorRepository.count() - apiMonitorRepository.findFailingMonitors().size();
        long apiFailing = apiMonitorRepository.findFailingMonitors().size();
        long apiAlerting = apiMonitorRepository.findAlertingMonitors().size();

        return List.of(
                MonitorSummaryResponse.builder()
                        .monitorType("site")
                        .total(siteTotal)
                        .healthy(siteHealthy)
                        .failing(siteFailing)
                        .alerting(siteAlerting)
                        .build(),
                MonitorSummaryResponse.builder()
                        .monitorType("api")
                        .total(apiTotal)
                        .healthy(apiHealthy)
                        .failing(apiFailing)
                        .alerting(apiAlerting)
                        .build()
        );
    }
}