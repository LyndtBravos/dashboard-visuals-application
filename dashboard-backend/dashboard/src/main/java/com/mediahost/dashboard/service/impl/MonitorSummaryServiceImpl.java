package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.dto.response.*;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.entity.MonitorAlert;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.MonitorType;
import com.mediahost.dashboard.repository.ApiMonitorRepository;
import com.mediahost.dashboard.repository.MonitorAlertRepository;
import com.mediahost.dashboard.repository.SiteMonitorRepository;
import com.mediahost.dashboard.service.MonitorSummaryService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitorSummaryServiceImpl implements MonitorSummaryService {

    @Autowired
    private SiteMonitorRepository siteMonitorRepository;

    @Autowired
    private ApiMonitorRepository apiMonitorRepository;

    @Autowired
    private MonitorAlertRepository alertRepository;

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

    @Override
    public List<MonitorAlertViewResponse> getActiveAlerts() {
        List<MonitorAlert> alerts = alertRepository.findByAcknowledgedFalse();

        return alerts.stream()
                .map(alert -> {
                    String name = getMonitorName(alert.getMonitorType(), alert.getMonitorId());

                    return MonitorAlertViewResponse.builder()
                            .id(alert.getId())
                            .monitorType(alert.getMonitorType())
                            .monitorId(alert.getMonitorId())
                            .name(name)
                            .severity(alert.getSeverity())
                            .failureReason(alert.getFailureReason())
                            .startedAt(alert.getStartedAt())
                            .lastOccurrence(alert.getLastOccurrence())
                            .occurrenceCount(alert.getOccurrenceCount())
                            .acknowledged(alert.getAcknowledged())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void acknowledgeAlert(Integer alertId) {
        MonitorAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found with id: " + alertId));

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }

    private String getMonitorName(MonitorType monitorType, Integer monitorId) {
        if (monitorType == MonitorType.site) {
            return siteMonitorRepository.findById(monitorId)
                    .map(SiteMonitor::getName)
                    .orElse("Unknown");
        } else {
            return apiMonitorRepository.findById(monitorId)
                    .map(ApiMonitor::getName)
                    .orElse("Unknown");
        }
    }
}