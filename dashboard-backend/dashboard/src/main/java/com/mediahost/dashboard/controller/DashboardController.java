package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.request.DashboardCreateRequest;
import com.mediahost.dashboard.model.dto.request.DashboardUpdateRequest;
import com.mediahost.dashboard.model.enums.ServiceType;
import com.mediahost.dashboard.model.dto.response.DashboardResponse;
import com.mediahost.dashboard.model.dto.response.ServiceSummaryResponse;
import com.mediahost.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "Get all dashboards")
    @GetMapping
    public ResponseEntity<List<DashboardResponse>> getAllDashboards(
            @RequestParam(required = false) ServiceType serviceType) {
        List<DashboardResponse> dashboards = dashboardService.getAllDashboards(serviceType);
        return ResponseEntity.ok(dashboards);
    }

    @Operation(summary = "Get dashboards for specific service")
    @GetMapping("/service/{type}")
    public ResponseEntity<List<DashboardResponse>> getDashboardsByService(@PathVariable ServiceType type) {
        List<DashboardResponse> dashboards = dashboardService.getDashboardsByService(type);
        return ResponseEntity.ok(dashboards);
    }

    @Operation(summary = "Get single dashboard")
    @GetMapping("/{id}")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable Integer id) {
        DashboardResponse dashboard = dashboardService.getDashboard(id);
        return ResponseEntity.ok(dashboard);
    }

    @Operation(summary = "Create new dashboard")
    @PostMapping
    public ResponseEntity<DashboardResponse> createDashboard(@Valid @RequestBody DashboardCreateRequest request,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        DashboardResponse created = dashboardService.createDashboard(request, userDetails.getUsername());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Update dashboard")
    @PutMapping("/{id}")
    public ResponseEntity<DashboardResponse> updateDashboard(@PathVariable Integer id,
            @Valid @RequestBody DashboardUpdateRequest request,@AuthenticationPrincipal UserDetails userDetails) {
        DashboardResponse updated = dashboardService.updateDashboard(id, request, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Soft delete (set is_active = false)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDashboard(@PathVariable Integer id) {
        dashboardService.deleteDashboard(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update active status")
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Integer id, @RequestParam boolean active) {
        dashboardService.updateStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get service summary with counts")
    @GetMapping("/service/{type}/summary")
    public ResponseEntity<ServiceSummaryResponse> getServiceSummary(@PathVariable ServiceType type) {
        ServiceSummaryResponse summary = dashboardService.getServiceSummary(type);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get summary for ALL services")
    @GetMapping("/summary")
    public ResponseEntity<List<ServiceSummaryResponse>> getAllServicesSummary() {
        List<ServiceSummaryResponse> summaries = dashboardService.getAllServicesSummary();
        return ResponseEntity.ok(summaries);
    }

    @Operation(summary = "Get all dashboards by service, order by flow order")
    @GetMapping("/service/{type}/ordered")
    public ResponseEntity<List<DashboardResponse>> getOrderedDashboardsByService(@PathVariable ServiceType type) {
        return ResponseEntity.ok(dashboardService.getOrderedDashboardsByService(type));
    }
}