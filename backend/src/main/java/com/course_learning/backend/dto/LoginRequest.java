package com.course_learning.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoginRequest {
    @Schema(example = "vijayk", description = "Username for authentication")
    private String userName;

    @Schema(example = "Admin@123", description = "Password for authentication")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
