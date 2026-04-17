package com.mediahost.dashboard.service.scheduler;

import com.mediahost.dashboard.service.EmailReportService;
import com.mediahost.dashboard.model.dto.request.EmailReportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;

@Service
@EnableScheduling
public class EmailScheduler {

    @Autowired
    private EmailReportService emailReportService;

    @Value("${dashboard.report.recipients:}")
    private String reportRecipients;

    @Value("${dashboard.report.enabled:false}")
    private boolean reportEnabled;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyDashboardReport() {
        if (!reportEnabled) return;

        if (reportRecipients == null || reportRecipients.isEmpty()) return;

        String[] recipients = reportRecipients.split(",");

        EmailReportRequest request = new EmailReportRequest();
        request.setSubject("Daily Dashboard Report - " + LocalDate.now());
        request.setMessage("Please find below the daily status report of all monitored visuals.");
        request.setStatusFilters(Arrays.asList("red", "yellow"));
        request.setIncludePdf(false);

        for (String recipient : recipients) {
            request.setTo(recipient.trim());
            try {
                emailReportService.sendDashboardReport(request);
            } catch (Exception e) {
                System.err.println("Failed to send report to " + recipient + ": " + e.getMessage());
            }
        }
    }
}