package com.course_learning.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.course_learning.backend.config.SecurityConstants;
import com.course_learning.backend.dto.AccountDeletionRequest;
import com.course_learning.backend.dto.ChangeEmailRequest;
import com.course_learning.backend.dto.ChangePasswordRequest;
import com.course_learning.backend.dto.ProfileUpdateRequest;
import com.course_learning.backend.dto.UserSettings;
import com.course_learning.backend.model.User;
import com.course_learning.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users, profiles, and account settings")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Get all users", description = "Retrieve a list of all users (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // delete user by userid
    @DeleteMapping("/{userId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN + " or " + SecurityConstants.IS_OWN_RESOURCE)
    @Operation(summary = "Delete user by ID", description = "Delete a user by their user ID (Admin or self only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteUserById(@PathVariable String userId) {
        boolean deleted = userService.deleteUserById(userId);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN + " or " + SecurityConstants.IS_OWN_RESOURCE)
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their user ID (Admin or self only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    // ========== PROFILE MANAGEMENT ENDPOINTS ==========

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieve the current authenticated user's profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required - Please provide a valid JWT token in Authorization header"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getCurrentUserProfile(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message",
                    "Authentication token is required. Please provide a valid JWT token in the Authorization header.",
                    "details", "Format: Authorization: Bearer <your-jwt-token>"));
        }

        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "USER_NOT_FOUND",
                        "message", "User profile not found"));
            }

            Map<String, Object> profile = Map.of(
                    "userId", user.getUserId(),
                    "userName", user.getUserName(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole(),
                    "isActive", user.isActive(),
                    "isVerified", user.isVerified(),
                    "createdAt", user.getCreatedAt(),
                    "updatedAt", user.getUpdatedAt());

            return ResponseEntity.ok(Map.of("success", true, "data", profile));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid token")) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "INVALID_TOKEN",
                        "message", "Invalid or expired authentication token. Please login again.",
                        "details", "Token format should be: Bearer <your-jwt-token>"));
            }
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_ERROR",
                    "message", "An unexpected error occurred while retrieving profile"));
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update the current user's profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid profile data - validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required - Please provide a valid JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateUserProfile(@RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ProfileUpdateRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message",
                    "Authentication token is required. Please provide a valid JWT token in the Authorization header.",
                    "details", "Format: Authorization: Bearer <your-jwt-token>"));
        }

        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            User updatedUser = userService.updateUserProfile(userId, request);
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Profile updated successfully", "data", updatedUser));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid token")) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "INVALID_TOKEN",
                        "message", "Invalid or expired authentication token. Please login again.",
                        "details", "Token format should be: Bearer <your-jwt-token>"));
            } else if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "USER_NOT_FOUND",
                        "message", "User account not found"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", "Profile update failed: " + e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password", description = "Change the current user's password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password data - wrong current password or validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required - Please provide a valid JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> changePassword(@RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message",
                    "Authentication token is required. Please provide a valid JWT token in the Authorization header.",
                    "details", "Format: Authorization: Bearer <your-jwt-token>"));
        }

        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.changePassword(userId, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("USER_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "USER_NOT_FOUND",
                        "message", "User account not found"));
            } else if (errorMessage.startsWith("INVALID_CURRENT_PASSWORD:")) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "INVALID_CURRENT_PASSWORD",
                        "message", "Current password is incorrect. Please enter your correct current password.",
                        "details", "Make sure you're entering the password you currently use to log in"));
            } else if (errorMessage.startsWith("PASSWORD_MISMATCH:")) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "PASSWORD_MISMATCH",
                        "message",
                        "New password and confirmation do not match. Please make sure both fields are identical.",
                        "details", "Type the same password in both 'New Password' and 'Confirm Password' fields"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", "Password change failed: " + errorMessage));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid token")) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "INVALID_TOKEN",
                        "message", "Invalid or expired authentication token. Please login again.",
                        "details", "Token format should be: Bearer <your-jwt-token>"));
            }
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "INTERNAL_ERROR",
                    "message", "An unexpected error occurred while changing password"));
        }
    }

    @PutMapping("/change-email")
    @Operation(summary = "Change email", description = "Change the current user's email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email change initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> changeEmail(@RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangeEmailRequest request) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.changeEmail(userId, request);
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "Email change initiated. Please verify your new email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/profile-picture")
    @Operation(summary = "Upload profile picture", description = "Upload a new profile picture for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> uploadProfilePicture(@RequestHeader("Authorization") String token,
            @RequestParam("profilePicture") MultipartFile file) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            String profilePictureUrl = userService.uploadProfilePicture(userId, file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile picture updated successfully",
                    "data", Map.of("profilePictureUrl", profilePictureUrl)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/profile-picture")
    @Operation(summary = "Delete profile picture", description = "Remove the current user's profile picture")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile picture deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteProfilePicture(@RequestHeader("Authorization") String token) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.deleteProfilePicture(userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile picture deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/settings")
    @Operation(summary = "Get user settings", description = "Retrieve the current user's settings and preferences")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getUserSettings(@RequestHeader("Authorization") String token) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            UserSettings settings = userService.getUserSettings(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", settings));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/settings")
    @Operation(summary = "Update user settings", description = "Update the current user's settings and preferences")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid settings data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateUserSettings(@RequestHeader("Authorization") String token,
            @Valid @RequestBody UserSettings settings) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.updateUserSettings(userId, settings);
            return ResponseEntity.ok(Map.of("success", true, "message", "Settings updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/account")
    @Operation(summary = "Delete user account", description = "Permanently delete the current user's account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deletion initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteUserAccount(@RequestHeader("Authorization") String token,
            @Valid @RequestBody AccountDeletionRequest request) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.deleteUserAccount(userId, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Account deletion initiated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify the user's email address with a verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid verification code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> verifyEmail(@RequestHeader("Authorization") String token,
            @RequestParam("verificationCode") String verificationCode) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.verifyEmail(userId, verificationCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Email verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification", description = "Resend the email verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> resendEmailVerification(@RequestHeader("Authorization") String token) {
        try {
            String userId = userService.getUserIdFromToken(token.replace("Bearer ", ""));
            userService.resendEmailVerification(userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Verification email sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is running")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    String healthCheck() {
        return "Hello";
    }
}
