package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.dto.request.ServerMonitorRequest;
import com.mediahost.dashboard.model.dto.response.ServerMonitorResponse;
import com.mediahost.dashboard.model.entity.*;
import com.mediahost.dashboard.model.enums.*;
import com.mediahost.dashboard.repository.*;
import com.mediahost.dashboard.service.ServerMonitorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerMonitorServiceImpl implements ServerMonitorService {

    private final ServerMonitorRepository serverMonitorRepository;
    private final UserRepository userRepository;
    private final MonitorAlertRepository alertRepository;

    @Override
    public List<ServerMonitorResponse> getAllServers(ServiceType serviceType) {
        List<ServerMonitor> servers = serviceType != null ?
                serverMonitorRepository.findByServiceTypeAndIsActiveTrue(serviceType) :
                serverMonitorRepository.findByIsActiveTrue();
        return servers.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ServerMonitorResponse getServerById(Integer id) {
        ServerMonitor server = serverMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server monitor not found: " + id));
        return convertToResponse(server);
    }

    @Override
    public List<ServerMonitorResponse> getFailingServers() {
        return serverMonitorRepository.findFailingMonitors().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ServerMonitorResponse> getAlertingServers() {
        return serverMonitorRepository.findAlertingMonitors().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServerMonitorResponse createServer(ServerMonitorRequest request, String username) {
        Integer userId = getUserByUsername(username);

        ServerMonitor server = new ServerMonitor();
        updateEntityFromRequest(server, request);
        server.setUpdatedBy(userId);

        ServerMonitor saved = serverMonitorRepository.save(server);
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public ServerMonitorResponse updateServer(Integer id, ServerMonitorRequest request, String username) {
        ServerMonitor existing = serverMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server monitor not found: " + id));

        Integer userId = getUserByUsername(username);

        updateEntityFromRequest(existing, request);
        existing.setUpdatedBy(userId);

        ServerMonitor saved = serverMonitorRepository.save(existing);
        return convertToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteServer(Integer id) {
        ServerMonitor server = serverMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server monitor not found: " + id));
        server.setIsActive(false);
        serverMonitorRepository.save(server);
        log.info("Server monitor {} deactivated", id);
    }

    @Override
    @Transactional
    public void resetFailureCount(Integer id) {
        ServerMonitor server = serverMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server monitor not found: " + id));
        server.setCurrentFailureCount(0);
        serverMonitorRepository.save(server);
        log.info("Server monitor {} failure count reset", id);
    }

    private boolean testTcpConnection(String host, int port, int timeoutSeconds) {
        int actualPort = port > 0 ? port : 80;
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, actualPort), timeoutSeconds * 1000);
            log.debug("TCP connection successful to {}:{}", host, actualPort);
            return true;
        } catch (Exception e) {
            log.debug("TCP connection failed to {}:{} - {}", host, actualPort, e.getMessage());
            return false;
        }
    }

    private boolean testHttpConnection(String host, int port, int timeoutSeconds, boolean useHttps) {
        try {
            String protocol = useHttps ? "https" : "http";
            int actualPort = port > 0 ? port : (useHttps ? 443 : 80);
            URL url = new URL(protocol + "://" + host + ":" + actualPort);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeoutSeconds * 1000);
            connection.setReadTimeout(timeoutSeconds * 1000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            boolean success = responseCode >= 200 && responseCode < 500;

            if (success)
                log.debug("HTTP connection successful to {}:{}, response code: {}", host, actualPort, responseCode);

            connection.disconnect();
            return success;
        } catch (Exception e) {
            log.debug("HTTP connection failed to {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }

    private boolean testIcmpPing(String host, int timeoutSeconds) {
        try {
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(timeoutSeconds * 1000);
            log.debug("ICMP ping to {}: {}", host, reachable ? "successful" : "failed");
            return reachable;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testSystemPing(String host, int timeoutSeconds) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;

            if (os.contains("win"))
                processBuilder = new ProcessBuilder("ping", "-n", "1", "-w", String.valueOf(timeoutSeconds * 1000), host);
            else
                processBuilder = new ProcessBuilder("ping", "-c", "1", "-W", String.valueOf(timeoutSeconds), host);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            log.debug("System ping to {}: {}", host, success ? "successful" : "failed");
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public ServerMonitorResponse recheckServer(Integer id) {
        ServerMonitor monitor = serverMonitorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server monitor not found: " + id));

        long startTime = System.currentTimeMillis();
        boolean reachable = false;
        String errorMessage = null;
        String successMethod = null;

        try {
            switch (monitor.getProtocol()) {
                case icmp:
                    reachable = testSystemPing(monitor.getHost(), monitor.getTimeoutSeconds());
                    if (reachable) {
                        successMethod = "System ping";
                        break;
                    }

                    reachable = testIcmpPing(monitor.getHost(), monitor.getTimeoutSeconds());
                    if (reachable) {
                        successMethod = "Java ICMP ping";
                        break;
                    }

                    int fallbackPort = monitor.getPort() != null ? monitor.getPort() : 80;
                    reachable = testTcpConnection(monitor.getHost(), fallbackPort, monitor.getTimeoutSeconds());
                    if (reachable) {
                        successMethod = "TCP fallback (ICMP blocked)";
                        errorMessage = "ICMP blocked, but TCP reachable on port " + fallbackPort;
                    }else errorMessage = "All connection methods failed (ICMP, System ping, TCP)";
                    break;

                case tcp:
                    int tcpPort = monitor.getPort() != null ? monitor.getPort() : 80;
                    reachable = testTcpConnection(monitor.getHost(), tcpPort, monitor.getTimeoutSeconds());
                    if (!reachable) errorMessage = "TCP connection failed on port " + tcpPort;
                    else successMethod = "TCP connection";
                    break;

                case http:
                    reachable = testHttpConnection(monitor.getHost(), monitor.getPort(), monitor.getTimeoutSeconds(), false);
                    if (!reachable) {
                        reachable = testTcpConnection(monitor.getHost(), monitor.getPort() != null ? monitor.getPort() : 80, monitor.getTimeoutSeconds());
                        if (reachable) {
                            successMethod = "TCP fallback (HTTP failed)";
                            errorMessage = "HTTP request failed, but TCP reachable";
                        } else errorMessage = "HTTP and TCP connections both failed";

                    } else
                        successMethod = "HTTP connection";
                    break;

                case https:
                    reachable = testHttpConnection(monitor.getHost(), monitor.getPort(), monitor.getTimeoutSeconds(), true);
                    if (!reachable) {
                        reachable = testTcpConnection(monitor.getHost(), monitor.getPort() != null ? monitor.getPort() : 443, monitor.getTimeoutSeconds());
                        if (reachable) {
                            successMethod = "TCP fallback (HTTPS failed)";
                            errorMessage = "HTTPS request failed, but TCP reachable";
                        } else
                            errorMessage = "HTTPS and TCP connections both failed";
                    }else
                        successMethod = "HTTPS connection";
                    break;
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Unexpected error during server check: {}", errorMessage);
        }

        long responseTime = System.currentTimeMillis() - startTime;

        monitor.setLastCheckTime(LocalDateTime.now());
        monitor.setResponseTimeMs((int) responseTime);

        if (reachable) {
            monitor.setLastCheckStatus(MonitorStatus.success);
            String message = "Server is reachable via " + successMethod;
            if (errorMessage != null)
                message += " (" + errorMessage + ")";

            monitor.setLastCheckMessage(message);
            log.info("Server {} check SUCCESS: {} - {}ms", monitor.getName(), message, responseTime);

            if (monitor.getCurrentFailureCount() > 0) {
                alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                        MonitorType.server, monitor.getId(), AlertStatus.failing
                ).ifPresent(alert -> {
                    alert.setCurrentStatus(AlertStatus.resolved);
                    alert.setResolvedAt(LocalDateTime.now());
                    alertRepository.save(alert);
                    log.info("Alert resolved for server {}", monitor.getName());
                });
                monitor.setCurrentFailureCount(0);
            }
        } else {
            int newFailureCount = monitor.getCurrentFailureCount() + 1;
            monitor.setCurrentFailureCount(newFailureCount);
            monitor.setLastCheckStatus(MonitorStatus.failed);
            String message = errorMessage != null ? errorMessage : "Connection failed - server may be offline or blocking traffic";
            monitor.setLastCheckMessage(message);
            log.warn("Server {} check FAILED (attempt {}/{}): {}",
                    monitor.getName(), newFailureCount, monitor.getRetryCount(), message);

            if (newFailureCount >= monitor.getRetryCount())
                createOrUpdateAlert(monitor, errorMessage);
        }

        ServerMonitor saved = serverMonitorRepository.save(monitor);
        return convertToResponse(saved);
    }

    public void checkDueServers(LocalDateTime date) {
        List<ServerMonitor> servers = serverMonitorRepository.findDueForCheck(date);
        log.info("Checking {} due servers", servers.size());
        servers.forEach(a -> recheckServer(a.getId()));
    }

    private void createOrUpdateAlert(ServerMonitor monitor, String errorMessage) {
        alertRepository.findByMonitorTypeAndMonitorIdAndCurrentStatus(
                MonitorType.server, monitor.getId(), AlertStatus.failing
        ).ifPresentOrElse(
                existingAlert -> {
                    existingAlert.setLastOccurrence(LocalDateTime.now());
                    existingAlert.setOccurrenceCount(existingAlert.getOccurrenceCount() + 1);
                    alertRepository.save(existingAlert);
                    log.debug("Updated existing alert for server {} (occurrence #{})",
                            monitor.getName(), existingAlert.getOccurrenceCount());
                },
                () -> {
                    MonitorAlert alert = new MonitorAlert();
                    alert.setMonitorType(MonitorType.server);
                    alert.setMonitorId(monitor.getId());
                    alert.setStartedAt(LocalDateTime.now());
                    alert.setLastOccurrence(LocalDateTime.now());
                    alert.setOccurrenceCount(1);
                    alert.setSeverity(monitor.getSeverity());
                    alert.setFailureReason("Server unreachable: " + (errorMessage != null ? errorMessage : "Connection timeout"));
                    alert.setCurrentStatus(AlertStatus.failing);
                    alert.setAcknowledged(false);
                    alertRepository.save(alert);
                    log.info("New alert created for server {} with severity {}",
                            monitor.getName(), monitor.getSeverity());
                }
        );
    }

    private Integer getUserByUsername(String username) {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        return user.getId();
    }

    private void updateEntityFromRequest(ServerMonitor entity, ServerMonitorRequest request) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setProtocol(request.getProtocol());
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

    private ServerMonitorResponse convertToResponse(ServerMonitor entity) {
        return ServerMonitorResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .host(entity.getHost())
                .port(entity.getPort())
                .protocol(entity.getProtocol())
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
                .responseTimeMs(entity.getResponseTimeMs())
                .currentFailureCount(entity.getCurrentFailureCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}