package com.course_learning.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.course_learning.backend.config.JwtUtil;

@Service
public class SecurityService {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Check if the current authenticated user owns the resource with the given userId
     */
    public boolean isCurrentUser(String userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            String currentUserId = getCurrentUserId();
            return currentUserId != null && currentUserId.equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the current authenticated user's ID from the JWT token
     */
    public String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String) {
                String token = (String) authentication.getCredentials();
                return jwtUtil.extractUserId(token);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the current authenticated user's role
     */
    public String getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities() != null) {
                return authentication.getAuthorities().iterator().next().getAuthority();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
