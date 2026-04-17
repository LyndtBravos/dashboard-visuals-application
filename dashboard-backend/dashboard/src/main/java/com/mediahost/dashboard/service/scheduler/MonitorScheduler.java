package com.mediahost.dashboard.service.scheduler;

import com.mediahost.dashboard.model.entity.*;
import com.mediahost.dashboard.model.enums.*;
import com.mediahost.dashboard.repository.*;
import com.mediahost.dashboard.service.ServerMonitorService;
import com.mediahost.dashboard.service.checker.*;
import com.mediahost.dashboard.util.BusinessHoursChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@EnableScheduling
@Slf4j
public class MonitorScheduler {

    @Autowired
    private SiteMonitorRepository siteMonitorRepository;

    @Autowired
    private ApiMonitorRepository apiMonitorRepository;

    @Autowired
    private MonitorHistoryRepository historyRepository;

    @Autowired
    private MonitorAlertRepository alertRepository;

    @Autowired
    private SiteChecker siteChecker;

    @Autowired
    private ApiChecker apiChecker;

    @Autowired
    private BusinessHoursChecker businessHoursChecker;

    @Autowired
    private ServerMonitorService serverMonitorService;

    @Scheduled(fixedDelay = 600_000)
    public void checkServerMonitors() {
        serverMonitorService.checkDueServers(LocalDateTime.now());
    }

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void checkAllMonitors() {
        if (!businessHoursChecker.shouldCheck()) {
            log.debug("Skipping monitor checks - outside business hours");
            return;
        }

        log.info("Starting monitor checks...");
        checkSiteMonitors();
        checkApiMonitors();
        log.info("Monitor checks completed");
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = historyRepository.deleteByCheckTimeBefore(cutoff);
        log.info("Cleaned up {} old history records", deleted);
    }

    private void checkSiteMonitors() {
        LocalDateTime now = LocalDateTime.now();
        List<SiteMonitor> dueSites = siteMonitorRepository.findDueForCheck(now);

        for (SiteMonitor site : dueSites)
            try {
                processSiteCheck(site);
            } catch (Exception e) {
                log.error("Error checking site monitor {}: {}", site.getId(), e.getMessage());
            }
    }

    private void checkApiMonitors() {
        LocalDateTime now = LocalDateTime.now();
        List<ApiMonitor> dueApis = apiMonitorRepository.findDueForCheck(now);

        for (ApiMonitor api : dueApis)
            try {
                processApiCheck(api);
            } catch (Exception e) {
                log.error("Error checking API monitor {}: {}", api.getId(), e.getMessage());
            }
    }

    private void processSiteCheck(SiteMonitor site) {
        boolean shouldCheck = businessHoursChecker.shouldCheck(
                site.getBusinessHoursOnly(),
                site.getBusinessHoursStart(),
                site.getBusinessHoursEnd(),
                site.getBusinessDays()
        );

        if (!shouldCheck)
            return;

        CheckResult result = siteChecker.check(
                site.getUrl(),
                site.getTimeoutSeconds(),
                site.getFollowRedirects()
        );

        boolean contentValid = true;
        if (result.getStatus() == MonitorStatus.success)
            contentValid = siteChecker.validateContent(result, site);

        MonitorStatus finalStatus = result.getStatus();
        if (result.getStatus() == MonitorStatus.success && !contentValid)
            finalStatus = MonitorStatus.failed;

        site.setLastCheckTime(LocalDateTime.now());
        site.setLastCheckStatus(finalStatus);
        site.setLastCheckMessage(result.getErrorMessage());

        if (finalStatus == MonitorStatus.success)
            handleSiteSuccess(site);
        else
            handleSiteFailure(site, result);

        siteMonitorRepository.save(site);
    }

    private void processApiCheck(ApiMonitor api) {
        boolean shouldCheck = businessHoursChecker.shouldCheck(
                api.getBusinessHoursOnly(),
                api.getBusinessHoursStart(),
                api.getBusinessHoursEnd(),
                api.getBusinessDays()
        );

        if (!shouldCheck) {
            api.setLastCheckTime(LocalDateTime.now());
            api.setLastCheckStatus(MonitorStatus.skipped);
            api.setLastCheckMessage("Skipped - outside business hours");
            apiMonitorRepository.save(api);
            return;
        }

        CheckResult result = apiChecker.check(api);

        boolean responseValid = true;
        if (result.getStatus() == MonitorStatus.success)
            responseValid = apiChecker.validateResponse(result, api);

        api.setLastCheckTime(LocalDateTime.now());
        api.setLastResponseTimeMs(result.getResponseTimeMs());
        api.setLastResponseSizeBytes(result.getResponseSizeBytes());
        api.setLastResponseBody(result.getResponseBody());

        MonitorStatus finalStatus = result.getStatus();
        if (result.getStatus() == MonitorStatus.success && !responseValid)
            finalStatus = MonitorStatus.failed;

        api.setLastCheckStatus(finalStatus);
        api.setLastCheckMessage(result.getErrorMessage());

        if (finalStatus == MonitorStatus.success)
            handleApiSuccess(api);
        else
            handleApiFailure(api, result);

        apiMonitorRepository.save(api);
    }

    private void handleSiteSuccess(SiteMonitor site) {
        if (site.getCurrentFailureCount() > 0) {
            alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                    MonitorType.site, site.getId(), AlertStatus.failing
            ).ifPresent(alert -> {
                alert.setCurrentStatus(AlertStatus.resolved);
                alert.setResolvedAt(LocalDateTime.now());
                alert.setAcknowledgedBy(1);
                alertRepository.save(alert);
                log.info("Alert resolved for site monitor {} after {} failures",
                        site.getId(), site.getCurrentFailureCount());
            });

            site.setCurrentFailureCount(0);
            log.info("Site monitor {} recovered", site.getId());
        }
    }

    private void handleApiSuccess(ApiMonitor api) {
        if (api.getCurrentFailureCount() > 0) {
            alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                    MonitorType.api, api.getId(), AlertStatus.failing
            ).ifPresent(alert -> {
                alert.setCurrentStatus(AlertStatus.resolved);
                alert.setResolvedAt(LocalDateTime.now());
                alertRepository.save(alert);
                log.info("Alert resolved for API monitor {}", api.getId());
            });

            api.setCurrentFailureCount(0);
            log.info("API monitor {} recovered", api.getId());
        }
    }

    private void handleSiteFailure(SiteMonitor site, CheckResult result) {
        int newFailureCount = site.getCurrentFailureCount() + 1;
        site.setCurrentFailureCount(newFailureCount);

        MonitorHistory history = new MonitorHistory();
        history.setMonitorType(MonitorType.site);
        history.setMonitorId(site.getId());
        history.setCheckTime(LocalDateTime.now());
        history.setStatus(site.getLastCheckStatus());
        history.setResponseTimeMs(result.getResponseTimeMs());
        history.setStatusCode(result.getStatusCode());
        history.setResponsePreview(result.getResponsePreview());
        history.setErrorMessage(result.getErrorMessage());
        history.setFailureCountAtTime(newFailureCount);
        history.setAlertCreated(false);

        if (newFailureCount >= site.getRetryCount()) {
            createOrUpdateAlert(MonitorType.site, site.getId(),
                    site.getSeverity(), result.getErrorMessage());
            history.setAlertCreated(true);
            log.warn("Alert created for site monitor {} after {} failures",
                    site.getId(), newFailureCount);
        }

        historyRepository.save(history);
    }

    private void handleApiFailure(ApiMonitor api, CheckResult result) {
        int newFailureCount = api.getCurrentFailureCount() + 1;
        api.setCurrentFailureCount(newFailureCount);

        MonitorHistory history = new MonitorHistory();
        history.setMonitorType(MonitorType.api);
        history.setMonitorId(api.getId());
        history.setCheckTime(LocalDateTime.now());
        history.setStatus(api.getLastCheckStatus());
        history.setResponseTimeMs(result.getResponseTimeMs());
        history.setStatusCode(result.getStatusCode());
        history.setResponsePreview(result.getResponsePreview());
        history.setErrorMessage(result.getErrorMessage());
        history.setFailureCountAtTime(newFailureCount);
        history.setAlertCreated(false);

        if (newFailureCount >= api.getRetryCount()) {
            createOrUpdateAlert(MonitorType.api, api.getId(),
                    api.getSeverity(), result.getErrorMessage());
            history.setAlertCreated(true);
            log.warn("Alert created for API monitor {} after {} failures",
                    api.getId(), newFailureCount);
        }

        historyRepository.save(history);
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
}