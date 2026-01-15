package com.course_learning.backend.config;

public class SecurityConstants {
    // Role constants
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_ADMIN = "ADMIN";

    // Role expressions for @PreAuthorize
    public static final String HAS_ROLE_STUDENT = "hasRole('" + ROLE_STUDENT + "')";
    public static final String HAS_ROLE_INSTRUCTOR = "hasRole('" + ROLE_INSTRUCTOR + "')";
    public static final String HAS_ROLE_ADMIN = "hasRole('" + ROLE_ADMIN + "')";

    // Combined role expressions
    public static final String HAS_ANY_ROLE = "hasAnyRole('" + ROLE_STUDENT + "', '" + ROLE_INSTRUCTOR + "', '" + ROLE_ADMIN + "')";
    public static final String HAS_INSTRUCTOR_OR_ADMIN = "hasAnyRole('" + ROLE_INSTRUCTOR + "', '" + ROLE_ADMIN + "')";
    public static final String HAS_ADMIN_ONLY = "hasRole('" + ROLE_ADMIN + "')";

    // Authentication expressions
    public static final String IS_AUTHENTICATED = "isAuthenticated()";
    public static final String IS_OWN_RESOURCE = "@securityService.isCurrentUser(#userId)";

    private SecurityConstants() {
        // Utility class
    }
}
