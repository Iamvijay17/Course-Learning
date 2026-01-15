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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.course_learning.backend.config.JwtUtil;
import com.course_learning.backend.dto.ModuleCreateRequest;
import com.course_learning.backend.dto.LessonCreateRequest;
import com.course_learning.backend.model.Module;
import com.course_learning.backend.model.Lesson;
import com.course_learning.backend.model.Video;
import com.course_learning.backend.service.ContentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/content")
@Tag(name = "Content Management", description = "APIs for managing course content including modules, lessons, and videos")
public class ContentController {

    @Autowired
    private ContentService contentService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========== MODULE ENDPOINTS (MOVED TO SectionController) ==========
    // Note: Module endpoints are now handled by SectionController for better organization

    @PutMapping("/modules/{moduleId}")
    @Operation(summary = "Update a module", description = "Update an existing module")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Module updated successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this module"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateModule(@RequestHeader(value = "Authorization", required = false) String token,
                                         @PathVariable String moduleId,
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

            Module moduleData = new Module();
            moduleData.setTitle(request.getTitle());
            moduleData.setDescription(request.getDescription());
            moduleData.setOrderIndex(request.getOrderIndex());

            Module updatedModule = contentService.updateModule(moduleId, moduleData, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Module updated successfully",
                "data", updatedModule
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("MODULE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "MODULE_NOT_FOUND",
                    "message", "Module not found"
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

    @DeleteMapping("/modules/{moduleId}")
    @Operation(summary = "Delete a module", description = "Delete a module and all its associated lessons and videos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Module deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to delete this module"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteModule(@RequestHeader(value = "Authorization", required = false) String token,
                                         @PathVariable String moduleId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            boolean deleted = contentService.deleteModule(moduleId, instructorId);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Module deleted successfully"
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "MODULE_NOT_FOUND",
                    "message", "Module not found"
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

    // ========== LESSON ENDPOINTS ==========

    @GetMapping("/modules/{moduleId}/lessons")
    @Operation(summary = "Get lessons for a module", description = "Retrieve all lessons for a specific module")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lessons retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to access this module")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getLessonsByModule(@RequestHeader(value = "Authorization", required = false) String token,
                                               @PathVariable String moduleId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            List<Lesson> lessons = contentService.getLessonsByModule(moduleId, instructorId);
            return ResponseEntity.ok(Map.of("success", true, "data", lessons));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("MODULE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "MODULE_NOT_FOUND",
                    "message", "Module not found"
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

    @PostMapping("/modules/{moduleId}/lessons")
    @Operation(summary = "Create a new lesson", description = "Create a new lesson for a specific module")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Lesson created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid lesson data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this module")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> createLesson(@RequestHeader(value = "Authorization", required = false) String token,
                                         @PathVariable String moduleId,
                                         @Valid @RequestBody LessonCreateRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Lesson lessonData = new Lesson();
            lessonData.setTitle(request.getTitle());
            lessonData.setDescription(request.getDescription());
            lessonData.setOrderIndex(request.getOrderIndex());
            lessonData.setIsPreview(request.getIsPreview());

            Lesson createdLesson = contentService.createLesson(moduleId, lessonData, instructorId);
            return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Lesson created successfully",
                "data", createdLesson
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("MODULE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "MODULE_NOT_FOUND",
                    "message", "Module not found"
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

    @PutMapping("/lessons/{lessonId}")
    @Operation(summary = "Update a lesson", description = "Update an existing lesson")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lesson updated successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this lesson"),
        @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateLesson(@RequestHeader(value = "Authorization", required = false) String token,
                                         @PathVariable String lessonId,
                                         @Valid @RequestBody LessonCreateRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Lesson lessonData = new Lesson();
            lessonData.setTitle(request.getTitle());
            lessonData.setDescription(request.getDescription());
            lessonData.setOrderIndex(request.getOrderIndex());
            lessonData.setIsPreview(request.getIsPreview());

            Lesson updatedLesson = contentService.updateLesson(lessonId, lessonData, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lesson updated successfully",
                "data", updatedLesson
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("LESSON_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "LESSON_NOT_FOUND",
                    "message", "Lesson not found"
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

    @DeleteMapping("/lessons/{lessonId}")
    @Operation(summary = "Delete a lesson", description = "Delete a lesson and its associated video")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lesson deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to delete this lesson"),
        @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteLesson(@RequestHeader(value = "Authorization", required = false) String token,
                                         @PathVariable String lessonId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            boolean deleted = contentService.deleteLesson(lessonId, instructorId);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lesson deleted successfully"
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "LESSON_NOT_FOUND",
                    "message", "Lesson not found"
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

    // ========== VIDEO ENDPOINTS ==========

    @PostMapping("/lessons/{lessonId}/video")
    @Operation(summary = "Upload video for a lesson", description = "Upload a video file for a specific lesson")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Video uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this lesson")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> uploadVideo(@RequestHeader(value = "Authorization", required = false) String token,
                                        @PathVariable String lessonId,
                                        @RequestParam("video") MultipartFile file) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            Video uploadedVideo = contentService.uploadVideo(lessonId, file, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Video uploaded successfully",
                "data", uploadedVideo
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("LESSON_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "LESSON_NOT_FOUND",
                    "message", "Lesson not found"
                ));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only modify your own courses"
                ));
            } else if (errorMessage.startsWith("VIDEO_EXISTS:")) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VIDEO_EXISTS",
                    "message", "Lesson already has a video. Delete existing video first."
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

    @GetMapping("/lessons/{lessonId}/video")
    @Operation(summary = "Get video for a lesson", description = "Retrieve video information for a specific lesson")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Video retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Video not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getVideoByLesson(@RequestHeader(value = "Authorization", required = false) String token,
                                             @PathVariable String lessonId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            // TODO: Add authorization check for video access
            Video video = contentService.getVideoByLesson(lessonId);
            if (video == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "VIDEO_NOT_FOUND",
                    "message", "Video not found for this lesson"
                ));
            }
            return ResponseEntity.ok(Map.of("success", true, "data", video));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @DeleteMapping("/lessons/{lessonId}/video")
    @Operation(summary = "Delete video for a lesson", description = "Delete the video associated with a specific lesson")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Video deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to delete this video"),
        @ApiResponse(responseCode = "404", description = "Video not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteVideo(@RequestHeader(value = "Authorization", required = false) String token,
                                        @PathVariable String lessonId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            boolean deleted = contentService.deleteVideo(lessonId, instructorId);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Video deleted successfully"
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "VIDEO_NOT_FOUND",
                    "message", "Video not found for this lesson"
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

    // ========== COURSE STRUCTURE ENDPOINTS ==========

    @GetMapping("/courses/{courseId}/structure")
    @Operation(summary = "Get course content structure", description = "Retrieve the complete content structure for a course including modules, lessons, and videos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Course structure retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to access this course"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getCourseContentStructure(@RequestHeader(value = "Authorization", required = false) String token,
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
            // TODO: Add authorization check for course access
            Map<String, Object> structure = contentService.getCourseContentStructure(courseId);
            return ResponseEntity.ok(Map.of("success", true, "data", structure));
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found"
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

    @GetMapping("/courses/{courseId}/preview")
    @Operation(summary = "Get course preview lessons", description = "Retrieve all preview lessons for a course (public access)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preview lessons retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<?> getCoursePreviewLessons(@PathVariable String courseId) {
        try {
            List<Lesson> previewLessons = contentService.getCoursePreviewLessons(courseId);
            return ResponseEntity.ok(Map.of("success", true, "data", previewLessons));
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", e.getMessage()
            ));
        }
    }

    // ========== REORDERING ENDPOINTS ==========

    @PutMapping("/courses/{courseId}/modules/reorder")
    @Operation(summary = "Reorder modules", description = "Reorder modules within a course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modules reordered successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this course")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> reorderModules(@RequestHeader(value = "Authorization", required = false) String token,
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
            List<String> moduleIds = request.get("moduleIds");
            if (moduleIds == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "INVALID_REQUEST",
                    "message", "moduleIds array is required"
                ));
            }

            contentService.reorderModules(courseId, moduleIds, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Modules reordered successfully"
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

    @PutMapping("/modules/{moduleId}/lessons/reorder")
    @Operation(summary = "Reorder lessons", description = "Reorder lessons within a module")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lessons reordered successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to modify this module")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> reorderLessons(@RequestHeader(value = "Authorization", required = false) String token,
                                           @PathVariable String moduleId,
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
            List<String> lessonIds = request.get("lessonIds");
            if (lessonIds == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "INVALID_REQUEST",
                    "message", "lessonIds array is required"
                ));
            }

            contentService.reorderLessons(moduleId, lessonIds, instructorId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lessons reordered successfully"
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("MODULE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "MODULE_NOT_FOUND",
                    "message", "Module not found"
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
