package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.request.ApiMonitorRequest;
import com.mediahost.dashboard.model.dto.response.ApiMonitorResponse;
import com.mediahost.dashboard.model.enums.ServiceType;
import com.mediahost.dashboard.service.ApiMonitorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors/apis")
public class ApiMonitorController {

    @Autowired
    private ApiMonitorService apiMonitorService;

    @GetMapping
    public ResponseEntity<List<ApiMonitorResponse>> getAllApis(
            @RequestParam(required = false) ServiceType serviceType) {
        return ResponseEntity.ok(apiMonitorService.getAllApis(serviceType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiMonitorResponse> getApi(@PathVariable Integer id) {
        return ResponseEntity.ok(apiMonitorService.getApiById(id));
    }

    @GetMapping("/failing")
    public ResponseEntity<List<ApiMonitorResponse>> getFailingApis() {
        return ResponseEntity.ok(apiMonitorService.getFailingApis());
    }

    @GetMapping("/alerting")
    public ResponseEntity<List<ApiMonitorResponse>> getAlertingApis() {
        return ResponseEntity.ok(apiMonitorService.getAlertingApis());
    }

    @PostMapping("/{id}/recheck")
    public ResponseEntity<ApiMonitorResponse> recheckMonitor(@PathVariable Integer id) {
        ApiMonitorResponse monitor = apiMonitorService.recheckApi(id);
        return ResponseEntity.ok(monitor);
    }

    @PostMapping
    public ResponseEntity<ApiMonitorResponse> createApi(@Valid @RequestBody ApiMonitorRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        ApiMonitorResponse response = apiMonitorService.createApi(request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiMonitorResponse> updateApi(@PathVariable Integer id,
            @Valid @RequestBody ApiMonitorRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        ApiMonitorResponse response = apiMonitorService.updateApi(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApi(@PathVariable Integer id) {
        apiMonitorService.deleteApi(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reset")
    public ResponseEntity<Void> resetFailureCount(@PathVariable Integer id) {
        apiMonitorService.resetFailureCount(id);
        return ResponseEntity.ok().build();
    }
}