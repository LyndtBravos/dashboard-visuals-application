package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.response.*;
import com.mediahost.dashboard.service.MonitorSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors/summary")
public class MonitorSummaryController {

    @Autowired
    private MonitorSummaryService monitorSummaryService;

    @GetMapping
    public ResponseEntity<List<MonitorSummaryResponse>> getSummary() {
        return ResponseEntity.ok(monitorSummaryService.getSummary());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<MonitorAlertViewResponse>> getActiveAlerts() {
        return ResponseEntity.ok(monitorSummaryService.getActiveAlerts());
    }

    @PutMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable Integer alertId) {
        monitorSummaryService.acknowledgeAlert(alertId);
        return ResponseEntity.ok().build();
    }
}