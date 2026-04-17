package com.mediahost.dashboard.service.impl;

import com.mediahost.dashboard.model.dto.request.DashboardCreateRequest;
import com.mediahost.dashboard.model.dto.request.DashboardUpdateRequest;
import com.mediahost.dashboard.model.dto.response.DashboardResponse;
import com.mediahost.dashboard.model.dto.response.ServiceSummaryResponse;
import com.mediahost.dashboard.model.entity.DashboardConfig;
import com.mediahost.dashboard.model.entity.User;
import com.mediahost.dashboard.model.enums.ServiceType;
import com.mediahost.dashboard.model.enums.StatusColor;

import com.mediahost.dashboard.repository.DashboardRepository;
import com.mediahost.dashboard.repository.UserRepository;
import com.mediahost.dashboard.service.DashboardService;
import com.mediahost.dashboard.service.QueryService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueryService queryService;

    @Override
    public List<DashboardResponse> getAllDashboards(ServiceType serviceType) {
        List<DashboardConfig> dashboards = dashboardRepository.findAllActive(serviceType);
        return dashboards.stream()
                .map(this::convertToResponseWithCurrentValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<DashboardResponse> getDashboardsByService(ServiceType serviceType) {
        List<DashboardConfig> dashboards = dashboardRepository.findByServiceTypeAndIsActiveTrue(serviceType);
        return dashboards.stream()
                .map(this::convertToResponseWithCurrentValue)
                .collect(Collectors.toList());
    }

    @Override
    public DashboardResponse getDashboard(Integer id) {
        DashboardConfig dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dashboard not found with id: " + id));
        return convertToResponseWithCurrentValue(dashboard);
    }

    @Override
    @Transactional
    public DashboardResponse createDashboard(DashboardCreateRequest request, String username) {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        DashboardConfig dashboard = new DashboardConfig();

        dashboard.setName(request.getName());
        dashboard.setDescription(request.getDescription());
        dashboard.setServiceType(request.getServiceType());
        dashboard.setGraphType(request.getGraphType());
        dashboard.setQueryText(request.getQueryText());
        dashboard.setThresholdWarning(request.getThresholdWarning());
        dashboard.setThresholdDanger(request.getThresholdDanger());
        dashboard.setAlertEmail(request.getAlertEmail());
        dashboard.setAlert(request.getAlert());
        dashboard.setWidth(request.getWidth());
        dashboard.setIsActive(true);
        dashboard.setUpdatedBy(user.getId());

        dashboard.setXAxisColumn(request.getXAxisColumn());
        dashboard.setYAxisColumns(request.getYAxisColumns());
        dashboard.setTimeInterval(request.getTimeInterval());
        dashboard.setSeriesColumns(request.getSeriesColumns());
        dashboard.setAggregationType(request.getAggregationType());

        DashboardConfig saved = dashboardRepository.save(dashboard);
        return convertToResponseWithCurrentValue(saved);
    }

    @Override
    @Transactional
    public DashboardResponse updateDashboard(Integer id, DashboardUpdateRequest request, String username) {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        DashboardConfig dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dashboard not found with id: " + id));

        if (request.getName() != null) dashboard.setName(request.getName());
        if (request.getDescription() != null) dashboard.setDescription(request.getDescription());
        if (request.getServiceType() != null) dashboard.setServiceType(request.getServiceType());
        if (request.getGraphType() != null) dashboard.setGraphType(request.getGraphType());
        if (request.getQueryText() != null) dashboard.setQueryText(request.getQueryText());
        if (request.getThresholdWarning() != null) dashboard.setThresholdWarning(request.getThresholdWarning());
        if (request.getThresholdDanger() != null) dashboard.setThresholdDanger(request.getThresholdDanger());
        if (request.getAlertEmail() != null) dashboard.setAlertEmail(request.getAlertEmail());
        if (request.getAlert() != null) dashboard.setAlert(request.getAlert());
        if (request.getWidth() != null) dashboard.setWidth(request.getWidth());
        if (request.getIsActive() != null) dashboard.setIsActive(request.getIsActive());
        if(request.getFlowOrder() != null) dashboard.setFlowOrder(request.getFlowOrder());

        if(request.getXAxisColumn() != null) dashboard.setXAxisColumn(request.getXAxisColumn());
        if(request.getYAxisColumns() != null) dashboard.setYAxisColumns(request.getYAxisColumns());
        if(request.getTimeInterval() != null) dashboard.setTimeInterval(request.getTimeInterval());
        if(request.getSeriesColumns() != null) dashboard.setSeriesColumns(request.getSeriesColumns());
        if(request.getAggregationType() != null) dashboard.setAggregationType(request.getAggregationType());

        dashboard.setUpdatedBy(user.getId());
        dashboard.setUpdatedAt(LocalDateTime.now());

        DashboardConfig updated = dashboardRepository.save(dashboard);
        return convertToResponseWithCurrentValue(updated);
    }

    @Override
    @Transactional
    public void deleteDashboard(Integer id) {
        DashboardConfig dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dashboard not found with id: " + id));
        dashboard.setIsActive(false);
        dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional
    public void updateStatus(Integer id, boolean active) {
        DashboardConfig dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dashboard not found with id: " + id));
        dashboard.setIsActive(active);
        dashboardRepository.save(dashboard);
    }

    @Override
    public ServiceSummaryResponse getServiceSummary(ServiceType serviceType) {
        List<DashboardConfig> dashboards = dashboardRepository.findByServiceTypeAndIsActiveTrue(serviceType);

        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("green", 0);
        statusCounts.put("yellow", 0);
        statusCounts.put("red", 0);

        for (DashboardConfig dashboard : dashboards)
            try {
                Object value = queryService.executeQuery(dashboard.getQueryText());
                StatusColor status = queryService.evaluateThreshold(
                        value,
                        dashboard.getThresholdWarning(),
                        dashboard.getThresholdDanger()
                );

                switch (status) {
                    case green: statusCounts.put("green", statusCounts.get("green") + 1); break;
                    case yellow: statusCounts.put("yellow", statusCounts.get("yellow") + 1); break;
                    case red: statusCounts.put("red", statusCounts.get("red") + 1); break;
                }
            } catch (Exception e) { statusCounts.put("red", statusCounts.get("red") + 1); }


        return ServiceSummaryResponse.builder()
                .serviceType(serviceType)
                .totalVisuals(dashboards.size())
                .statusCounts(statusCounts)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<ServiceSummaryResponse> getAllServicesSummary() {
        List<ServiceSummaryResponse> summaries = new ArrayList<>();
        for (ServiceType type : ServiceType.values()) summaries.add(getServiceSummary(type));
        return summaries;
    }

    @Override
    public List<DashboardResponse> getOrderedDashboardsByService(ServiceType serviceType) {
        List<DashboardConfig> dashboards = dashboardRepository.findByServiceTypeAndIsActiveTrue(serviceType);

        dashboards.sort((a, b) -> {
            if (a.getFlowOrder() != null && b.getFlowOrder() != null)
                return a.getFlowOrder().compareTo(b.getFlowOrder());
            if (a.getFlowOrder() != null) return -1;
            if (b.getFlowOrder() != null) return 1;
            return a.getName().compareTo(b.getName());
        });
        return dashboards.stream()
                .map(this::convertToResponseWithCurrentValue)
                .collect(Collectors.toList());
    }

    private DashboardResponse convertToResponseWithCurrentValue(DashboardConfig dashboard) {
        DashboardResponse.DashboardResponseBuilder builder = DashboardResponse.builder()
                .id(dashboard.getId())
                .name(dashboard.getName())
                .description(dashboard.getDescription())
                .serviceType(dashboard.getServiceType())
                .graphType(dashboard.getGraphType())
                .queryText(dashboard.getQueryText())
                .thresholdWarning(dashboard.getThresholdWarning())
                .thresholdDanger(dashboard.getThresholdDanger())
                .alertEmail(dashboard.getAlertEmail())
                .alert(dashboard.getAlert())
                .width(dashboard.getWidth())
                .isActive(dashboard.getIsActive())
                .createdAt(dashboard.getCreatedAt())
                .updatedAt(dashboard.getUpdatedAt())
                .updatedBy(dashboard.getUpdatedBy())
                .aggregationType(dashboard.getAggregationType())
                .xAxisColumn(dashboard.getXAxisColumn())
                .yAxisColumn(dashboard.getYAxisColumns())
                .timeInterval(dashboard.getTimeInterval())
                .seriesColumns(dashboard.getSeriesColumns())
                .flowOrder(dashboard.getFlowOrder());

        try {
            Object value = queryService.executeQuery(dashboard.getQueryText());
            StatusColor status = queryService.evaluateThreshold(
                    value,
                    dashboard.getThresholdWarning(),
                    dashboard.getThresholdDanger()
            );

            builder.currentValue(value)
                    .currentStatus(status)
                    .lastChecked(LocalDateTime.now());
        } catch (Exception e) {
            builder.currentValue("Error")
                    .currentStatus(StatusColor.red)
                    .lastChecked(LocalDateTime.now());
        }

        return builder.build();
    }
}