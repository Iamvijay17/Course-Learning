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
import com.course_learning.backend.dto.LoginRequest;
import com.course_learning.backend.dto.LoginResponse;
import com.course_learning.backend.model.User;
import com.course_learning.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
                "active", user.isActive()
            ));
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
                "userName", createdUser.getUserName()
            ));
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
                    "verified", user.isVerified()
                ));
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
}
