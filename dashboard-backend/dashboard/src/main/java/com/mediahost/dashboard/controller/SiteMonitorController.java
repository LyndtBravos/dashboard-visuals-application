package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.request.SiteMonitorRequest;
import com.mediahost.dashboard.model.dto.response.SiteMonitorResponse;
import com.mediahost.dashboard.model.enums.ServiceType;
import com.mediahost.dashboard.service.SiteMonitorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors/sites")
public class SiteMonitorController {

    @Autowired
    private SiteMonitorService siteMonitorService;

    @GetMapping
    public ResponseEntity<List<SiteMonitorResponse>> getAllSites(@RequestParam(required = false) ServiceType serviceType) {
        return ResponseEntity.ok(siteMonitorService.getAllSites(serviceType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteMonitorResponse> getSite(@PathVariable Integer id) {
        return ResponseEntity.ok(siteMonitorService.getSiteById(id));
    }

    @GetMapping("/failing")
    public ResponseEntity<List<SiteMonitorResponse>> getFailingSites() {
        return ResponseEntity.ok(siteMonitorService.getFailingSites());
    }

    @GetMapping("/alerting")
    public ResponseEntity<List<SiteMonitorResponse>> getAlertingSites() {
        return ResponseEntity.ok(siteMonitorService.getAlertingSites());
    }

    @PostMapping
    public ResponseEntity<SiteMonitorResponse> createSite(@Valid @RequestBody SiteMonitorRequest request,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        SiteMonitorResponse response = siteMonitorService.createSite(request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/recheck")
    public ResponseEntity<SiteMonitorResponse> recheckMonitor(@PathVariable Integer id) {
        SiteMonitorResponse monitor = siteMonitorService.recheckSite(id);
        return ResponseEntity.ok(monitor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiteMonitorResponse> updateSite(@PathVariable Integer id,
            @Valid @RequestBody SiteMonitorRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        SiteMonitorResponse response = siteMonitorService.updateSite(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Integer id) {
        siteMonitorService.deleteSite(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reset")
    public ResponseEntity<Void> resetFailureCount(@PathVariable Integer id) {
        siteMonitorService.resetFailureCount(id);
        return ResponseEntity.ok().build();
    }
}