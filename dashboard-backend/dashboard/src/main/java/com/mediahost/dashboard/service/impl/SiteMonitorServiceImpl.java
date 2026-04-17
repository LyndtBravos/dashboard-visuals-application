package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.entity.MonitorAlert;
import com.mediahost.dashboard.model.entity.MonitorHistory;
import com.mediahost.dashboard.model.enums.*;
import com.mediahost.dashboard.model.dto.request.SiteMonitorRequest;
import com.mediahost.dashboard.model.dto.response.SiteMonitorResponse;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.entity.User;
import com.mediahost.dashboard.repository.MonitorAlertRepository;
import com.mediahost.dashboard.repository.MonitorHistoryRepository;
import com.mediahost.dashboard.repository.SiteMonitorRepository;
import com.mediahost.dashboard.repository.UserRepository;
import com.mediahost.dashboard.service.SiteMonitorService;
import com.mediahost.dashboard.service.checker.CheckResult;
import com.mediahost.dashboard.service.checker.SiteChecker;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SiteMonitorServiceImpl implements SiteMonitorService {

    @Autowired
    private SiteMonitorRepository siteMonitorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MonitorAlertRepository alertRepository;

    @Autowired
    private MonitorHistoryRepository historyRepository;

    @Autowired
    private SiteChecker siteChecker;

    @Override
    public List<SiteMonitorResponse> getAllSites(ServiceType serviceType) {
        List<SiteMonitor> sites = serviceType != null ?
                siteMonitorRepository.findByServiceTypeAndIsActiveTrue(serviceType) :
                siteMonitorRepository.findByIsActiveTrue();
        return sites.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SiteMonitorResponse getSiteById(Integer id) {
        SiteMonitor site = siteMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site monitor not found: " + id));
        return convertToResponse(site);
    }

    @Override
    public List<SiteMonitorResponse> getFailingSites() {
        return siteMonitorRepository.findFailingMonitors().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SiteMonitorResponse> getAlertingSites() {
        return siteMonitorRepository.findAlertingMonitors().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SiteMonitorResponse createSite(SiteMonitorRequest request, String username) {
        Integer userId = getUserByUsername(username);

        SiteMonitor site = new SiteMonitor();
        updateEntityFromRequest(site, request);
        site.setUpdatedBy(userId);

        SiteMonitor saved = siteMonitorRepository.save(site);
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public SiteMonitorResponse updateSite(Integer id, SiteMonitorRequest request, String username) {
        SiteMonitor existing = siteMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site monitor not found: " + id));

        Integer userId = getUserByUsername(username);

        updateEntityFromRequest(existing, request);
        existing.setUpdatedBy(userId);

        SiteMonitor saved = siteMonitorRepository.save(existing);
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSite(Integer id) {
        SiteMonitor site = siteMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site monitor not found: " + id));
        site.setIsActive(false);
        siteMonitorRepository.save(site);
    }

    @Override
    @Transactional
    public void resetFailureCount(Integer id) {
        SiteMonitor site = siteMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site monitor not found: " + id));
        site.setCurrentFailureCount(0);
        siteMonitorRepository.save(site);
    }

    @Override
    @Transactional
    public SiteMonitorResponse recheckSite(Integer id) {
        SiteMonitor monitor = getSiteEntity(id);

        CheckResult result = siteChecker.check(
                monitor.getUrl(),
                monitor.getTimeoutSeconds(),
                monitor.getFollowRedirects()
        );

        boolean contentValid = true;
        if (result.getStatus() == MonitorStatus.success)
            contentValid = siteChecker.validateContent(result, monitor);

        MonitorStatus finalStatus = result.getStatus();
        if (result.getStatus() == MonitorStatus.success && !contentValid)
            finalStatus = MonitorStatus.failed;

        monitor.setLastCheckTime(LocalDateTime.now());
        monitor.setLastCheckStatus(finalStatus);
        monitor.setLastCheckMessage(result.getErrorMessage());

        if (finalStatus == MonitorStatus.success) {
            if (monitor.getCurrentFailureCount() > 0) {
                alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                        MonitorType.site, monitor.getId(), AlertStatus.failing
                ).ifPresent(alert -> {
                    alert.setCurrentStatus(AlertStatus.resolved);
                    alert.setResolvedAt(LocalDateTime.now());
                    alertRepository.save(alert);
                });
                monitor.setCurrentFailureCount(0);
            }
        } else {
            int newFailureCount = monitor.getCurrentFailureCount() + 1;
            monitor.setCurrentFailureCount(newFailureCount);

            MonitorHistory history = new MonitorHistory();
            history.setMonitorType(MonitorType.site);
            history.setMonitorId(monitor.getId());
            history.setCheckTime(LocalDateTime.now());
            history.setStatus(finalStatus);
            history.setResponseTimeMs(result.getResponseTimeMs());
            history.setStatusCode(result.getStatusCode());
            history.setResponsePreview(result.getResponsePreview());
            history.setErrorMessage(result.getErrorMessage());
            history.setFailureCountAtTime(newFailureCount);
            history.setAlertCreated(false);

            if (newFailureCount >= monitor.getRetryCount()) {
                createOrUpdateAlert(MonitorType.site, monitor.getId(),
                        monitor.getSeverity(), result.getErrorMessage());
                history.setAlertCreated(true);
            }

            historyRepository.save(history);
        }

        SiteMonitor saved = siteMonitorRepository.save(monitor);
        return convertToResponse(saved);
    }

    public void createOrUpdateAlert(MonitorType type, Integer monitorId,
                                    Severity severity, String failureReason) {
        alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                type, monitorId, AlertStatus.failing
        ).ifPresentOrElse(
                existingAlert -> {
                    existingAlert.setLastOccurrence(LocalDateTime.now());
                    existingAlert.setOccurrenceCount(existingAlert.getOccurrenceCount() + 1);
                    alertRepository.save(existingAlert);
                    log.debug("Updated existing alert for monitor {} (occurrence #{})",
                            monitorId, existingAlert.getOccurrenceCount());
                },
                () -> {
                    MonitorAlert alert = new MonitorAlert();
                    alert.setMonitorType(type);
                    alert.setMonitorId(monitorId);
                    alert.setStartedAt(LocalDateTime.now());
                    alert.setLastOccurrence(LocalDateTime.now());
                    alert.setOccurrenceCount(1);
                    alert.setSeverity(severity);
                    alert.setFailureReason(failureReason);
                    alert.setCurrentStatus(AlertStatus.failing);
                    alert.setAcknowledged(false);
                    alertRepository.save(alert);
                    log.info("New alert created for monitor {} with severity {}",
                            monitorId, severity);
                }
        );
    }

    private SiteMonitor getSiteEntity(Integer id) {
        return siteMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site monitor not found: " + id));
    }

    private Integer getUserByUsername(String username) {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return user.getId();
    }

    private void updateEntityFromRequest(SiteMonitor entity, SiteMonitorRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setUrl(request.getUrl());
        entity.setExpectedPhrase(request.getExpectedPhrase());
        entity.setExpectedPhraseMissing(request.getExpectedPhraseMissing());
        entity.setRetryCount(request.getRetryCount());
        entity.setRetryIntervalSeconds(request.getRetryIntervalSeconds());
        entity.setCheckIntervalMinutes(request.getCheckIntervalMinutes());
        entity.setTimeoutSeconds(request.getTimeoutSeconds());
        entity.setFollowRedirects(request.getFollowRedirects());
        entity.setExpectedStatusCode(request.getExpectedStatusCode());
        entity.setServiceType(request.getServiceType());
        entity.setSeverity(request.getSeverity());
        entity.setBusinessHoursOnly(request.getBusinessHoursOnly());
        entity.setBusinessHoursStart(request.getBusinessHoursStart());
        entity.setBusinessHoursEnd(request.getBusinessHoursEnd());
        entity.setBusinessDays(request.getBusinessDays());
        entity.setIsActive(request.getIsActive());
        entity.setAlert(request.getAlert());
        entity.setAlertEmail(request.getAlertEmail());
    }

    private SiteMonitorResponse convertToResponse(SiteMonitor entity) {
        return SiteMonitorResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .url(entity.getUrl())
                .expectedPhrase(entity.getExpectedPhrase())
                .expectedPhraseMissing(entity.getExpectedPhraseMissing())
                .retryCount(entity.getRetryCount())
                .retryIntervalSeconds(entity.getRetryIntervalSeconds())
                .checkIntervalMinutes(entity.getCheckIntervalMinutes())
                .timeoutSeconds(entity.getTimeoutSeconds())
                .followRedirects(entity.getFollowRedirects())
                .expectedStatusCode(entity.getExpectedStatusCode())
                .serviceType(entity.getServiceType())
                .severity(entity.getSeverity())
                .businessHoursOnly(entity.getBusinessHoursOnly())
                .businessHoursStart(entity.getBusinessHoursStart())
                .businessHoursEnd(entity.getBusinessHoursEnd())
                .businessDays(entity.getBusinessDays())
                .isActive(entity.getIsActive())
                .alert(entity.getAlert())
                .alertEmail(entity.getAlertEmail())
                .lastCheckTime(entity.getLastCheckTime())
                .lastCheckStatus(entity.getLastCheckStatus())
                .lastCheckMessage(entity.getLastCheckMessage())
                .currentFailureCount(entity.getCurrentFailureCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}