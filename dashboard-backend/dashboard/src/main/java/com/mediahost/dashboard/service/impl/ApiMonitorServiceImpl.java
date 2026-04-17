package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.entity.*;
import com.mediahost.dashboard.model.enums.*;
import com.mediahost.dashboard.model.dto.request.ApiMonitorRequest;
import com.mediahost.dashboard.model.dto.response.ApiMonitorResponse;
import com.mediahost.dashboard.repository.ApiMonitorRepository;
import com.mediahost.dashboard.repository.MonitorAlertRepository;
import com.mediahost.dashboard.repository.MonitorHistoryRepository;
import com.mediahost.dashboard.repository.UserRepository;
import com.mediahost.dashboard.service.ApiMonitorService;
import com.mediahost.dashboard.service.checker.ApiChecker;
import com.mediahost.dashboard.service.checker.CheckResult;
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
public class ApiMonitorServiceImpl implements ApiMonitorService {

    @Autowired
    private ApiMonitorRepository apiMonitorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiChecker apiChecker;

    @Autowired
    private MonitorAlertRepository alertRepository;

    @Autowired
    private MonitorHistoryRepository historyRepository;

    @Override
    public List<ApiMonitorResponse> getAllApis(ServiceType serviceType) {
        List<ApiMonitor> apis = serviceType != null ?
                apiMonitorRepository.findByServiceTypeAndIsActiveTrue(serviceType) :
                apiMonitorRepository.findByIsActiveTrue();
        return apis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ApiMonitorResponse getApiById(Integer id) {
        ApiMonitor api = apiMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API monitor not found: " + id));
        return convertToResponse(api);
    }

    @Override
    public List<ApiMonitorResponse> getFailingApis() {
        return apiMonitorRepository.findFailingMonitors().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiMonitorResponse> getAlertingApis() {
        return apiMonitorRepository.findAlertingMonitors().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiMonitorResponse createApi(ApiMonitorRequest request, String username) {
        Integer userId = getUserByUsername(username);

        ApiMonitor api = new ApiMonitor();
        updateEntityFromRequest(api, request);
        api.setUpdatedBy(userId);

        ApiMonitor saved = apiMonitorRepository.save(api);
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public ApiMonitorResponse updateApi(Integer id, ApiMonitorRequest request, String username) {
        ApiMonitor existing = apiMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API monitor not found: " + id));

        Integer userId = getUserByUsername(username);

        updateEntityFromRequest(existing, request);
        existing.setUpdatedBy(userId);

        ApiMonitor saved = apiMonitorRepository.save(existing);
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteApi(Integer id) {
        ApiMonitor api = apiMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API monitor not found: " + id));
        api.setIsActive(false);
        apiMonitorRepository.save(api);
    }

    @Override
    @Transactional
    public void resetFailureCount(Integer id) {
        ApiMonitor api = apiMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API monitor not found: " + id));
        api.setCurrentFailureCount(0);
        apiMonitorRepository.save(api);
    }

    @Override
    @Transactional
    public ApiMonitorResponse recheckApi(Integer id) {
        ApiMonitor monitor = getApiEntity(id);

        CheckResult result = apiChecker.check(monitor);

        boolean responseValid = true;
        if (result.getStatus() == MonitorStatus.success)
            responseValid = apiChecker.validateResponse(result, monitor);

        MonitorStatus finalStatus = result.getStatus();
        if (result.getStatus() == MonitorStatus.success && !responseValid)
            finalStatus = MonitorStatus.failed;

        monitor.setLastCheckTime(LocalDateTime.now());
        monitor.setLastCheckStatus(finalStatus);
        monitor.setLastCheckMessage(result.getErrorMessage());
        monitor.setLastResponseTimeMs(result.getResponseTimeMs());
        monitor.setLastResponseSizeBytes(result.getResponseSizeBytes());
        monitor.setLastResponseBody(result.getResponseBody());

        if (finalStatus == MonitorStatus.success) {
            if (monitor.getCurrentFailureCount() > 0) {
                alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                        MonitorType.api, monitor.getId(), AlertStatus.failing
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
            history.setMonitorType(MonitorType.api);
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
                createOrUpdateAlert(MonitorType.api, monitor.getId(),
                        monitor.getSeverity(), result.getErrorMessage());
                history.setAlertCreated(true);
            }

            historyRepository.save(history);
        }

        return convertToResponse(apiMonitorRepository.save(monitor));
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

    private ApiMonitor getApiEntity(Integer id) {
        return apiMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API monitor not found: " + id));
    }

    private Integer getUserByUsername(String username) {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return user.getId();
    }

    private void updateEntityFromRequest(ApiMonitor entity, ApiMonitorRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setUrl(request.getUrl());
        entity.setMethod(request.getMethod());
        entity.setRequestHeadersJson(request.getRequestHeadersJson());
        entity.setRequestBody(request.getRequestBody());
        entity.setRequestContentType(request.getRequestContentType());
        entity.setExpectedStatusCode(request.getExpectedStatusCode());
        entity.setExpectedResponseTimeMs(request.getExpectedResponseTimeMs());
        entity.setExpectedResponseSizeBytes(request.getExpectedResponseSizeBytes());
        entity.setExpectedResponseContains(request.getExpectedResponseContains());
        entity.setExpectedJsonPath(request.getExpectedJsonPath());
        entity.setExpectedValue(request.getExpectedValue());
        entity.setTimeoutSeconds(request.getTimeoutSeconds());
        entity.setRetryCount(request.getRetryCount());
        entity.setRetryIntervalSeconds(request.getRetryIntervalSeconds());
        entity.setCheckIntervalMinutes(request.getCheckIntervalMinutes());
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

    private ApiMonitorResponse convertToResponse(ApiMonitor entity) {
        return ApiMonitorResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .url(entity.getUrl())
                .method(entity.getMethod())
                .requestHeadersJson(entity.getRequestHeadersJson())
                .requestBody(entity.getRequestBody())
                .requestContentType(entity.getRequestContentType())
                .expectedStatusCode(entity.getExpectedStatusCode())
                .expectedResponseTimeMs(entity.getExpectedResponseTimeMs())
                .expectedResponseSizeBytes(entity.getExpectedResponseSizeBytes())
                .expectedResponseContains(entity.getExpectedResponseContains())
                .expectedJsonPath(entity.getExpectedJsonPath())
                .expectedValue(entity.getExpectedValue())
                .timeoutSeconds(entity.getTimeoutSeconds())
                .retryCount(entity.getRetryCount())
                .retryIntervalSeconds(entity.getRetryIntervalSeconds())
                .checkIntervalMinutes(entity.getCheckIntervalMinutes())
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
                .lastResponseTimeMs(entity.getLastResponseTimeMs())
                .lastResponseSizeBytes(entity.getLastResponseSizeBytes())
                .currentFailureCount(entity.getCurrentFailureCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}