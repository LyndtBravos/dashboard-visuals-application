package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.request.ServerMonitorRequest;
import com.mediahost.dashboard.model.dto.response.ServerMonitorResponse;
import com.mediahost.dashboard.model.enums.ServiceType;
import com.mediahost.dashboard.service.ServerMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors/servers")
@RequiredArgsConstructor
public class ServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    @GetMapping
    public ResponseEntity<List<ServerMonitorResponse>> getAllServers(
            @RequestParam(required = false) ServiceType serviceType) {
        return ResponseEntity.ok(serverMonitorService.getAllServers(serviceType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerMonitorResponse> getServer(@PathVariable Integer id) {
        return ResponseEntity.ok(serverMonitorService.getServerById(id));
    }

    @GetMapping("/failing")
    public ResponseEntity<List<ServerMonitorResponse>> getFailingServers() {
        return ResponseEntity.ok(serverMonitorService.getFailingServers());
    }

    @GetMapping("/alerting")
    public ResponseEntity<List<ServerMonitorResponse>> getAlertingServers() {
        return ResponseEntity.ok(serverMonitorService.getAlertingServers());
    }

    @PostMapping
    public ResponseEntity<ServerMonitorResponse> createServer(
            @Valid @RequestBody ServerMonitorRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ServerMonitorResponse created = serverMonitorService.createServer(request, userDetails.getUsername());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerMonitorResponse> updateServer(
            @PathVariable Integer id,
            @Valid @RequestBody ServerMonitorRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(serverMonitorService.updateServer(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Integer id) {
        serverMonitorService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reset")
    public ResponseEntity<Void> resetFailureCount(@PathVariable Integer id) {
        serverMonitorService.resetFailureCount(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/recheck")
    public ResponseEntity<ServerMonitorResponse> recheckServer(@PathVariable Integer id) {
        ServerMonitorResponse monitor = serverMonitorService.recheckServer(id);
        return ResponseEntity.ok(monitor);
    }
}