package com.mediahost.dashboard.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.mediahost.dashboard.model.enums.*;
import java.time.LocalTime;

@Data
public class ServerMonitorRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Host is required")
    @Pattern(regexp = "^([a-zA-Z0-9.-]+|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})$",
            message = "Invalid hostname or IP address")
    private String host;

    @Min(1) @Max(65535)
    private Integer port;

    private ServerProtocol protocol = ServerProtocol.icmp;

    @Min(1) @Max(60)
    private Integer timeoutSeconds = 5;

    @Min(0) @Max(10)
    private Integer retryCount = 2;

    @Min(5) @Max(3600)
    private Integer retryIntervalSeconds = 30;

    @Min(1) @Max(1440)
    private Integer checkIntervalMinutes = 5;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Severity is required")
    private Severity severity = Severity.medium;

    private Boolean businessHoursOnly = false;
    private LocalTime businessHoursStart;
    private LocalTime businessHoursEnd;
    private String businessDays;
    private Boolean isActive = true;
    private Boolean alert = true;

    @Email(message = "Invalid email format")
    private String alertEmail;
}