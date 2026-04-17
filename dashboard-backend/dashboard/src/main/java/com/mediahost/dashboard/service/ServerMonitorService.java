package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.request.ServerMonitorRequest;
import com.mediahost.dashboard.model.dto.response.ServerMonitorResponse;
import com.mediahost.dashboard.model.enums.ServiceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ServerMonitorService {

    List<ServerMonitorResponse> getAllServers(ServiceType serviceType);

    ServerMonitorResponse getServerById(Integer id);

    List<ServerMonitorResponse> getFailingServers();

    List<ServerMonitorResponse> getAlertingServers();

    ServerMonitorResponse createServer(ServerMonitorRequest request, String username);

    ServerMonitorResponse updateServer(Integer id, ServerMonitorRequest request, String username);

    void deleteServer(Integer id);

    void resetFailureCount(Integer id);

    ServerMonitorResponse recheckServer(Integer id);

    void checkDueServers(LocalDateTime date);
}