package com.course_learning.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Course Learning Platform API",
        version = "1.0",
        description = """
            Backend API for Course Learning Platform with JWT Authentication and Role-Based Access Control

            ## 🚀 Getting Started

            1. **Login first** using the `/api/auth/login` endpoint
            2. **Copy the token** from the login response
            3. **Click 'Authorize' button** (top right)
            4. **Enter**: `Bearer your-jwt-token-here`
            5. **Test authenticated endpoints**

            ## 👥 Test Accounts
            - **Admin**: `vijayk` / `Admin@123`
            - **Student**: `testuser` / `password123`

            ## 🔐 Authentication Required
            Most endpoints require JWT authentication. Look for the 🔒 icon.

            ## 📋 Error Handling
            All endpoints return structured error responses with specific error codes for better debugging.
            """
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authorization header using the Bearer scheme. Enter your JWT token in the format: Bearer {token}"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication. Get token from /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Course Learning Platform API")
                        .version("1.0")
                        .description("""
                            ## 📚 Course Learning Platform API

                            A comprehensive REST API for an online learning platform with role-based access control.

                            ### 🔑 Authentication Flow
                            1. **POST** `/api/auth/login` - Get JWT token
                            2. **Click** 'Authorize' button in Swagger UI
                            3. **Enter** token: `Bearer your-jwt-token-here`
                            4. **Test** protected endpoints

                            ### 👤 Available Roles
                            - **STUDENT**: Course enrollment, profile management
                            - **INSTRUCTOR**: Course creation, student management
                            - **ADMIN**: Full system administration

                            ### 🧪 Test Credentials
                            | Role | Username | Password |
                            |------|----------|----------|
                            | Admin | vijayk | Admin@123 |
                            | Student | testuser | password123 |

                            ### ⚠️ Error Responses
                            All errors include specific codes and user-friendly messages for better debugging.
                            """)
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Course Platform Team")
                                .email("support@courseplatform.com")));
    }
}
