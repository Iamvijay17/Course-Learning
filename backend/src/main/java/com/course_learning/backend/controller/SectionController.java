package com.course_learning.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.config.JwtUtil;
import com.course_learning.backend.dto.ModuleCreateRequest;
import com.course_learning.backend.model.Section;
import com.course_learning.backend.service.SectionService;
import com.course_learning.backend.service.ContentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sections")
@Tag(name = "Section Management", description = "APIs for managing course sections, including CRUD operations and ordering")
public class SectionController {

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ContentService contentService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Get sections for a course", description = "Retrieve all sections for a specific course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sections retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to access this course")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getSectionsByCourse(@RequestHeader(value = "Authorization", required = false) String token,
                                                @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            List<Section> sections = sectionService.getSectionsByCourse(courseId, instructorId);
            return ResponseEntity.ok(Map.of("success", true, "data", sections));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", errorMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @PostMapping("/courses/{courseId}")
    @Operation(summary = "Create a new section", description = "Create a new section for a specific course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Section created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid section data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this course")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> createSection(@RequestHeader(value = "Authorization", required = false) String token,
                                          @PathVariable String courseId,
                                          @Valid @RequestBody Section sectionData) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Section createdSection = sectionService.createSection(courseId, sectionData, instructorId);
            return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Section created successfully",
                "data", createdSection
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only modify your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", errorMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @PutMapping("/{sectionId}")
    @Operation(summary = "Update a section", description = "Update an existing section")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Section updated successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this section"),
        @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateSection(@RequestHeader(value = "Authorization", required = false) String token,
                                          @PathVariable String sectionId,
                                          @Valid @RequestBody Section sectionData) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Section updatedSection = sectionService.updateSection(sectionId, sectionData, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Section updated successfully",
                "data", updatedSection
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("SECTION_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "SECTION_NOT_FOUND",
                    "message", "Section not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only modify your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", errorMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "Delete a section", description = "Delete a section and all its associated modules, lessons, and videos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Section deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to delete this section"),
        @ApiResponse(responseCode = "404", description = "Section not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteSection(@RequestHeader(value = "Authorization", required = false) String token,
                                          @PathVariable String sectionId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            boolean deleted = sectionService.deleteSection(sectionId, instructorId);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Section deleted successfully"
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "SECTION_NOT_FOUND",
                    "message", "Section not found"
                ));
            }
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only modify your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    // ========== SECTION ORDERING ==========

    @PutMapping("/courses/{courseId}/reorder")
    @Operation(summary = "Reorder sections", description = "Reorder sections within a course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sections reordered successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this course")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> reorderSections(@RequestHeader(value = "Authorization", required = false) String token,
                                            @PathVariable String courseId,
                                            @RequestBody Map<String, List<String>> request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            List<String> sectionIds = request.get("sectionIds");
            if (sectionIds == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "INVALID_REQUEST",
                    "message", "sectionIds array is required"
                ));
            }

            sectionService.reorderSections(courseId, sectionIds, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sections reordered successfully"
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only modify your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", errorMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    // ========== MODULE OPERATIONS WITHIN SECTIONS ==========

    @GetMapping("/{sectionId}/modules")
    @Operation(summary = "Get modules for a section", description = "Retrieve all modules for a specific section")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modules retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to access this section")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getModulesBySection(@RequestHeader(value = "Authorization", required = false) String token,
                                                @PathVariable String sectionId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            List<com.course_learning.backend.model.Module> modules = contentService.getModulesBySection(sectionId, instructorId);
            return ResponseEntity.ok(Map.of("success", true, "data", modules));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("SECTION_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "SECTION_NOT_FOUND",
                    "message", "Section not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", errorMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @PostMapping("/{sectionId}/modules")
    @Operation(summary = "Create a new module in a section", description = "Create a new module for a specific section")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Module created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid module data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this section")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> createModule(@RequestHeader(value = "Authorization", required = false) String token,
                                         @PathVariable String sectionId,
                                         @Valid @RequestBody ModuleCreateRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            com.course_learning.backend.model.Module moduleData = new com.course_learning.backend.model.Module();
            moduleData.setTitle(request.getTitle());
            moduleData.setDescription(request.getDescription());
            moduleData.setOrderIndex(request.getOrderIndex());

            com.course_learning.backend.model.Module createdModule = contentService.createModule(sectionId, moduleData, instructorId);
            return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Module created successfully",
                "data", createdModule
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("SECTION_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "SECTION_NOT_FOUND",
                    "message", "Section not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only modify your own courses"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", errorMessage
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }
}
