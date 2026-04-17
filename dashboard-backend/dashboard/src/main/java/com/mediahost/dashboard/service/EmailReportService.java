package com.mediahost.dashboard.service;

import com.mediahost.dashboard.model.dto.request.EmailReportRequest;
import com.mediahost.dashboard.model.dto.response.DashboardResponse;
import com.mediahost.dashboard.model.enums.ServiceType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailReportService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private DashboardService dashboardService;

    @Value("${spring.mail.from}")
    private String fromEmail;

    public void sendDashboardReport(EmailReportRequest request) {
        try {
            List<DashboardResponse> visuals = getVisualsByStatus(request);
            String htmlContent = generateHtmlReport(request, visuals);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject() + " - " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email report: " + e.getMessage(), e);
        }
    }

    private List<DashboardResponse> getVisualsByStatus(EmailReportRequest request) {
        List<DashboardResponse> allVisuals;

        if (request.getServiceType() != null && !request.getServiceType().isEmpty())
            allVisuals = dashboardService.getDashboardsByService(
                    ServiceType.valueOf(request.getServiceType()));
        else
            allVisuals = dashboardService.getAllDashboards(null);

        if (request.getStatusFilters().contains("all"))
            return allVisuals;

        return allVisuals.stream()
                .filter(v -> request.getStatusFilters()
                        .contains(v.getCurrentStatus().name()))
                .collect(Collectors.toList());
    }

    private String generateHtmlReport(EmailReportRequest request, List<DashboardResponse> visuals) {
        Context context = new Context();
        context.setVariable("reportDate", LocalDateTime.now());
        context.setVariable("statusFilters", String.join(", ", request.getStatusFilters()
                .stream().map(String::toUpperCase).toList()));
        context.setVariable("serviceType", request.getServiceType() != null ?
                request.getServiceType() : "All Services");
        context.setVariable("visuals", visuals);
        context.setVariable("totalVisuals", visuals.size());
        context.setVariable("message", request.getMessage());

        long greenCount = visuals.stream()
                .filter(v -> v.getCurrentStatus() != null &&
                        v.getCurrentStatus().name().equalsIgnoreCase("green"))
                .count();
        long yellowCount = visuals.stream()
                .filter(v -> v.getCurrentStatus() != null &&
                        v.getCurrentStatus().name().equalsIgnoreCase("yellow"))
                .count();
        long redCount = visuals.stream()
                .filter(v -> v.getCurrentStatus() != null &&
                        v.getCurrentStatus().name().equalsIgnoreCase("red"))
                .count();

        context.setVariable("greenCount", greenCount);
        context.setVariable("yellowCount", yellowCount);
        context.setVariable("redCount", redCount);

        return templateEngine.process("dashboard-report", context);
    }
}