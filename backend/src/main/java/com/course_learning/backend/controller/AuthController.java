package com.course_learning.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.config.JwtUtil;
import com.course_learning.backend.dto.ForgotPasswordRequest;
import com.course_learning.backend.dto.LoginRequest;
import com.course_learning.backend.dto.ResetPasswordRequest;
import com.course_learning.backend.dto.VerifyResetTokenRequest;
import com.course_learning.backend.model.User;
import com.course_learning.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication APIs for users and admins")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "User/Admin login", description = "Authenticate user or admin and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.login(loginRequest.getUserName(), loginRequest.getPassword());
        if (token != null) {
            // Get user details for role information
            User user = userService.getUserByUserName(loginRequest.getUserName());
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                    "userId", user.getUserId(),
                    "userName", user.getUserName(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole(),
                    "active", user.isActive()));
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid user data")
    })
    public ResponseEntity<?> register(@RequestBody User userData) {
        try {
            User createdUser = userService.createUser(userData);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "userId", createdUser.getUserId(),
                    "userName", createdUser.getUserName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();

            User user = userService.getUserByUserName(userName);
            if (user != null) {
                return ResponseEntity.ok(Map.of(
                        "userId", user.getUserId(),
                        "userName", user.getUserName(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "role", user.getRole(),
                        "active", user.isActive(),
                        "verified", user.isVerified()));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout the current user (JWT is stateless, so this just returns success)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> logout() {
        // Since JWT is stateless, we just return success
        // In a production app, you might want to implement token blacklisting
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ========== PASSWORD RESET ENDPOINTS ==========

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Send password reset instructions to user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset instructions sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password reset instructions sent to your email",
                    "details", "Please check your email and follow the reset link"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_ERROR",
                    "message", "Failed to process password reset request"));
        }
    }

    @PostMapping("/verify-reset-token")
    @Operation(summary = "Verify password reset token", description = "Verify if the reset token is valid and not expired")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "400", description = "Token is invalid or expired")
    })
    public ResponseEntity<?> verifyResetToken(@Valid @RequestBody VerifyResetTokenRequest request) {
        try {
            boolean isValid = userService.verifyResetToken(request.getToken(), request.getEmail());
            if (isValid) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Reset token is valid",
                        "data", Map.of("tokenValid", true)));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "INVALID_RESET_TOKEN",
                        "message", "Reset token is invalid or expired"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_ERROR",
                    "message", "Failed to verify reset token"));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token", description = "Reset user password using valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token, password, or validation failed")
    })
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPasswordWithToken(
                    request.getToken(),
                    request.getEmail(),
                    request.getNewPassword(),
                    request.getConfirmPassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password reset successfully. You can now login with your new password."));
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("INVALID_RESET_TOKEN:")) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "INVALID_RESET_TOKEN",
                        "message", "Reset token is invalid or expired"));
            } else if (errorMessage.startsWith("PASSWORD_MISMATCH:")) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "PASSWORD_MISMATCH",
                        "message",
                        "New password and confirmation do not match. Please make sure both fields are identical.",
                        "details", "Type the same password in both 'New Password' and 'Confirm Password' fields"));
            } else if (errorMessage.startsWith("USER_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "USER_NOT_FOUND",
                        "message", "User account not found"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", "Password reset failed: " + errorMessage));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_ERROR",
                    "message", "An unexpected error occurred during password reset"));
        }
    }
}
