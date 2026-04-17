package com.mediahost.dashboard.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "UserID is required")
    @Size(min = 3, max = 3, message = "UserID must be exactly 3 characters")
    @Pattern(regexp = "^[A-Za-z0-9]{3}$", message = "UserID must be alphanumeric")
    private String userId;

    @NotBlank(message = "Password is required")
    @Size(min = 3, max = 20, message = "Password must be between 3 and 20 characters")
    private String password;
}