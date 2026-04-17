package com.mediahost.dashboard.controller;

import com.mediahost.dashboard.model.dto.request.EmailReportRequest;
import com.mediahost.dashboard.service.EmailReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailReportService emailReportService;

    @PostMapping("/send-report")
    public ResponseEntity<Map<String, String>> sendDashboardReport(
            @Valid @RequestBody EmailReportRequest request) {
        emailReportService.sendDashboardReport(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Report sent successfully to " + request.getTo());
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }
}