package com.course_learning.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ChangeEmailRequest {
    @NotBlank(message = "New email is required")
    @Email(message = "New email must be valid")
    private String newEmail;

    @NotBlank(message = "Password is required for email change")
    private String password;

    // Constructors
    public ChangeEmailRequest() {}

    public ChangeEmailRequest(String newEmail, String password) {
        this.newEmail = newEmail;
        this.password = password;
    }

    // Getters and Setters
    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
