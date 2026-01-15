package com.course_learning.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccountDeletionRequest {
    @NotBlank(message = "Password is required for account deletion")
    private String password;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    // Constructors
    public AccountDeletionRequest() {}

    public AccountDeletionRequest(String password, String reason) {
        this.password = password;
        this.reason = reason;
    }

    // Getters and Setters
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
