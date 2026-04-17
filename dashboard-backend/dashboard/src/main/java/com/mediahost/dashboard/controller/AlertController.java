package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.response.AlertDetailResponse;
import com.mediahost.dashboard.model.dto.response.MonitorAlertViewResponse;
import com.mediahost.dashboard.model.enums.AlertStatus;
import com.mediahost.dashboard.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<MonitorAlertViewResponse>> getAllAlerts(
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(required = false) AlertStatus status) {
        return ResponseEntity.ok(alertService.getAllAlerts(acknowledged, status));
    }

    @GetMapping("/active")
    public ResponseEntity<List<MonitorAlertViewResponse>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getAlertCount() {
        return ResponseEntity.ok(alertService.getAlertCount());
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<AlertDetailResponse> getAlertDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(alertService.getAlertDetail(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorAlertViewResponse> getAlert(@PathVariable Integer id) {
        return ResponseEntity.ok(alertService.getAlert(id));
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        alertService.acknowledgeAlert(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Integer id) {
        alertService.resolveAlert(id);
        return ResponseEntity.ok().build();
    }
}