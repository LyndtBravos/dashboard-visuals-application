package com.mediahost.dashboard.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class EmailReportRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    private String subject = "Dashboard Services Report";

    private String message = "Dashboard Services Report";

    @NotNull(message = "Status filter is required")
    private List<String> statusFilters;

    private boolean includePdf = false;

    private String serviceType;
}