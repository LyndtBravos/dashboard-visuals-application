package com.mediahost.dashboard.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.mediahost.dashboard.model.enums.*;

@Data
public class DashboardUpdateRequest {

    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private ServiceType serviceType;

    private GraphType graphType;

    @Size(min = 5, max = 5000, message = "Query must be between 5 and 5000 characters")
    private String queryText;

    @DecimalMin(value = "0.0", inclusive = false, message = "Warning threshold must be positive")
    private Double thresholdWarning;

    @DecimalMin(value = "0.0", inclusive = false, message = "Danger threshold must be positive")
    private Double thresholdDanger;

    @Email(message = "Alert email must be valid")
    private String alertEmail;

    private Boolean alert;

    private Width width;

    private Boolean isActive;

    private String xAxisColumn;

    @Size(max = 1000, message = "Y-axis columns cannot exceed 1000 characters")
    private String yAxisColumns;

    @Size(max = 1000, message = "Series columns cannot exceed 1000 characters")
    private String seriesColumns;

    private AggregationType aggregationType;

    @Size(max = 50, message = "Time interval cannot exceed 50 characters")
    private String timeInterval;
    private Integer flowOrder;

    @AssertTrue(message = "If alert is enabled, alert email is required")
    private boolean isValidAlertConfig() {
        if (Boolean.TRUE.equals(alert)) return alertEmail != null && !alertEmail.trim().isEmpty();
        return true;
    }

    @AssertTrue(message = "Warning threshold must be less than danger threshold when both are provided")
    private boolean isValidThresholds() {
        if (thresholdWarning == null || thresholdDanger == null) return true;
        return thresholdWarning < thresholdDanger;
    }
}