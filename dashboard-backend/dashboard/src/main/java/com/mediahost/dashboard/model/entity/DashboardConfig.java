package com.mediahost.dashboard.model.entity;

import com.mediahost.dashboard.model.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_configs")
@Data
public class DashboardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    @Column(length = 200, nullable = false)
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Service type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @NotNull(message = "Graph type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "graph_type", nullable = false)
    private GraphType graphType;

    @NotBlank(message = "Query text is required")
    @Size(min = 5, max = 5000, message = "Query must be between 5 and 5000 characters")
    @Column(name = "query_text", columnDefinition = "TEXT", nullable = false)
    private String queryText;

    @DecimalMin(value = "0.0", inclusive = false, message = "Warning threshold must be positive")
    @DecimalMax(value = "999999.99", message = "Warning threshold too high")
    @Column(name = "threshold_warning", columnDefinition = "DECIMAL")
    private Double thresholdWarning;

    @DecimalMin(value = "0.0", inclusive = false, message = "Danger threshold must be positive")
    @DecimalMax(value = "999999.99", message = "Danger threshold too high")
    @Column(name = "threshold_danger", columnDefinition = "DECIMAL")
    private Double thresholdDanger;

    @Email(message = "Alert email must be valid")
    @Size(max = 255, message = "Alert email cannot exceed 255 characters")
    @Column(name = "alert_email")
    private String alertEmail;

    @NotNull(message = "Alert flag is required")
    @Column(name = "alert")
    private Boolean alert = false;

    @NotNull(message = "Width is required")
    @Enumerated(EnumType.STRING)
    private Width width = Width.medium;

    @PastOrPresent(message = "Created at must be in the past or present")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PastOrPresent(message = "Updated at must be in the past or present")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "x_axis_column")
    private String xAxisColumn;

    @Column(name = "y_axis_columns", columnDefinition = "TEXT")
    private String yAxisColumns;

    @Column(name = "series_columns", columnDefinition = "TEXT")
    private String seriesColumns;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type")
    private AggregationType aggregationType;

    @Column(name = "time_interval", length = 50)
    private String timeInterval;

    @Column(name = "flow_order")
    private Integer flowOrder;

    @AssertTrue(message = "Warning threshold must be less than danger threshold")
    private boolean isValidThresholds() {
        if (thresholdWarning == null || thresholdDanger == null) return true;

        return thresholdWarning < thresholdDanger;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}