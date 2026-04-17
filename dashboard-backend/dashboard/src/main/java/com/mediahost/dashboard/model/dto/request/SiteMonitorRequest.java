package com.mediahost.dashboard.model.dto.request;


import com.mediahost.dashboard.model.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalTime;

@Data
public class SiteMonitorRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(http|https)://.*$", message = "Invalid URL format")
    private String url;

    private String expectedPhrase;
    private Boolean expectedPhraseMissing = false;

    @Min(0) @Max(10)
    private Integer retryCount = 3;

    @Min(5) @Max(3600)
    private Integer retryIntervalSeconds = 60;

    @Min(1) @Max(1440)
    private Integer checkIntervalMinutes = 5;

    @Min(1) @Max(120)
    private Integer timeoutSeconds = 30;

    private Boolean followRedirects = true;

    @Min(100) @Max(599)
    private Integer expectedStatusCode = 200;

    @NotNull
    private ServiceType serviceType;

    private Severity severity = Severity.medium;

    private Boolean businessHoursOnly = false;
    private LocalTime businessHoursStart = LocalTime.of(6, 0);
    private LocalTime businessHoursEnd = LocalTime.of(20, 0);
    private String businessDays = "1,2,3,4,5";

    private Boolean isActive = true;
    private Boolean alert = true;

    @Email
    private String alertEmail;
}