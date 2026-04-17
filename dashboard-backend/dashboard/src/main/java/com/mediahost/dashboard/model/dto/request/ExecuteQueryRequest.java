package com.mediahost.dashboard.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExecuteQueryRequest {

    @NotBlank(message = "Query is required")
    @Size(min = 5, max = 5000, message = "Query must be between 5 and 5000 characters")
    @Pattern(regexp = "(?i)^SELECT.*", message = "Only SELECT queries are allowed")
    private String query;
    private Double warningThreshold;
    private Double dangerThreshold;
}