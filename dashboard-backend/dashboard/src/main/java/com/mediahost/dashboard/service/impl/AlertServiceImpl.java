package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.dto.response.AlertDetailResponse;
import com.mediahost.dashboard.model.dto.response.MonitorAlertViewResponse;
import com.mediahost.dashboard.model.entity.ApiMonitor;
import com.mediahost.dashboard.model.entity.MonitorAlert;
import com.mediahost.dashboard.model.entity.SiteMonitor;
import com.mediahost.dashboard.model.enums.AlertStatus;
import com.mediahost.dashboard.model.enums.MonitorType;
import com.mediahost.dashboard.repository.*;
import com.mediahost.dashboard.service.AlertService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final MonitorAlertRepository alertRepository;
    private final SiteMonitorRepository siteMonitorRepository;
    private final ApiMonitorRepository apiMonitorRepository;
    private final UserRepository userRepository;
    private final ServerMonitorRepository serverMonitorRepository;
    private final DashboardRepository dashboardRepository;

    @Override
    public List<MonitorAlertViewResponse> getAllAlerts(Boolean acknowledged, AlertStatus status) {
        List<MonitorAlert> alerts;

        if (acknowledged != null)
            alerts = alertRepository.findByAcknowledged(acknowledged);
        else if (status != null)
            alerts = alertRepository.findByCurrentStatus(status);
        else
            alerts = alertRepository.findAll();

        return alerts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MonitorAlertViewResponse> getActiveAlerts() {
        List<MonitorAlert> alerts = alertRepository.findByCurrentStatusAndAcknowledgedFalse(AlertStatus.failing);
        return alerts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MonitorAlertViewResponse getAlert(Integer id) {
        MonitorAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found: " + id));
        return convertToResponse(alert);
    }

    @Override
    @Transactional
    public void acknowledgeAlert(Integer alertId, String username) {
        MonitorAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found: " + alertId));

        Integer userId = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"))
                .getId();

        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alert {} acknowledged by {}", alertId, username);
    }

    @Override
    @Transactional
    public void resolveAlert(Integer alertId) {
        MonitorAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found: " + alertId));

        alert.setCurrentStatus(AlertStatus.resolved);
        alert.setResolvedAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alert {} resolved", alertId);
    }

    @Override
    @Transactional
    public Map<String, Long> getAlertCount() {
        List<MonitorAlert> list = alertRepository.findAll();
        Map<String, Long> map = list
                .stream()
                .collect(Collectors
                        .groupingBy(a -> a.getSeverity().toString(),
                                Collectors.counting()));
        map.put("total", (long) list.size());
        return map;
    }

    @Override
    public AlertDetailResponse getAlertDetail(Integer id) {
        MonitorAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found: " + id));

        AlertDetailResponse.AlertDetailResponseBuilder builder = AlertDetailResponse.builder()
                .id(alert.getId())
                .monitorType(alert.getMonitorType())
                .monitorId(alert.getMonitorId())
                .severity(alert.getSeverity())
                .failureReason(alert.getFailureReason())
                .startedAt(alert.getStartedAt())
                .lastOccurrence(alert.getLastOccurrence())
                .occurrenceCount(alert.getOccurrenceCount())
                .acknowledged(alert.getAcknowledged())
                .resolvedAt(alert.getResolvedAt());

        if (alert.getAcknowledgedBy() != null) {
            userRepository.findById(alert.getAcknowledgedBy())
                    .ifPresent(user -> builder.acknowledgedBy(user.getUserId()));
            builder.acknowledgedAt(alert.getAcknowledgedAt());
        }

        if (alert.getMonitorType() == MonitorType.site)
            siteMonitorRepository.findById(alert.getMonitorId()).ifPresent(site ->
                builder.name(site.getName())
                        .url(site.getUrl())
                        .serviceType(site.getServiceType().toString())
                        .expectedStatusCode(site.getExpectedStatusCode())
                        .currentFailureCount(site.getCurrentFailureCount())
                        .retryCount(site.getRetryCount())
                        .lastCheckMessage(site.getLastCheckMessage())
                        .lastCheckTime(site.getLastCheckTime()));
        else if (alert.getMonitorType() == MonitorType.api)
            apiMonitorRepository.findById(alert.getMonitorId()).ifPresent(api ->
                builder.name(api.getName())
                        .url(api.getUrl())
                        .method(api.getMethod().toString())
                        .serviceType(api.getServiceType().toString())
                        .expectedStatusCode(api.getExpectedStatusCode())
                        .currentFailureCount(api.getCurrentFailureCount())
                        .retryCount(api.getRetryCount())
                        .lastCheckMessage(api.getLastCheckMessage())
                        .lastCheckTime(api.getLastCheckTime()));
        else if (alert.getMonitorType() == MonitorType.server)
            serverMonitorRepository.findById(alert.getMonitorId()).ifPresent(server ->
                builder.name(server.getName())
                        .host(server.getHost())
                        .port(server.getPort())
                        .protocol(server.getProtocol().toString())
                        .serviceType(server.getServiceType().toString())
                        .currentFailureCount(server.getCurrentFailureCount())
                        .retryCount(server.getRetryCount())
                        .lastCheckMessage(server.getLastCheckMessage())
                        .lastCheckTime(server.getLastCheckTime()));
        else if (alert.getMonitorType() == MonitorType.query)
            dashboardRepository.findById(alert.getMonitorId()).ifPresent(dashboard ->
                builder.name(dashboard.getName())
                        .queryText(dashboard.getQueryText())
                        .graphType(dashboard.getGraphType().toString())
                        .serviceType(dashboard.getServiceType().toString())
                        .currentFailureCount(null)
                        .retryCount(null)
                        .lastCheckMessage(null)
                        .lastCheckTime(null));

        return builder.build();
    }


    private MonitorAlertViewResponse convertToResponse(MonitorAlert alert) {
        String monitorName;

        if (alert.getMonitorType() == MonitorType.site)
            monitorName = siteMonitorRepository.findById(alert.getMonitorId())
                    .map(SiteMonitor::getName).orElse("Unknown");
         else
            monitorName = apiMonitorRepository.findById(alert.getMonitorId())
                    .map(ApiMonitor::getName).orElse("Unknown");


        return MonitorAlertViewResponse.builder()
                .id(alert.getId())
                .monitorType(alert.getMonitorType())
                .monitorId(alert.getMonitorId())
                .name(monitorName)
                .severity(alert.getSeverity())
                .failureReason(alert.getFailureReason())
                .startedAt(alert.getStartedAt())
                .lastOccurrence(alert.getLastOccurrence())
                .occurrenceCount(alert.getOccurrenceCount())
                .acknowledged(alert.getAcknowledged())
                .acknowledgedBy(alert.getAcknowledgedBy() == null ? 0 : alert.getAcknowledgedBy())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}