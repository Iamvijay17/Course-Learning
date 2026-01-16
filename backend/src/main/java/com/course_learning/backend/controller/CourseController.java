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
import com.course_learning.backend.dto.CourseCreateRequest;
import com.course_learning.backend.dto.CourseUpdateRequest;
import com.course_learning.backend.model.Course;
import com.course_learning.backend.service.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Course Management", description = "APIs for managing courses, including CRUD operations, publishing, and search")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========== PUBLIC ENDPOINTS ==========

    @GetMapping("/published")
    @Operation(summary = "Get all published courses", description = "Retrieve a list of all published courses available to students")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<?> getPublishedCourses() {
        List<Course> courses = courseService.getPublishedCourses();
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    @GetMapping("/published/{courseId}")
    @Operation(summary = "Get published course by ID", description = "Retrieve a specific published course by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<?> getPublishedCourseById(@PathVariable String courseId) {
        Course course = courseService.getCourseById(courseId);
        if (course == null || !course.getIsPublished()) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found or not published"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", course));
    }

    @GetMapping("/search")
    @Operation(summary = "Search courses", description = "Search for published courses by keyword")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    public ResponseEntity<?> searchCourses(@RequestParam(required = false) String keyword) {
        List<Course> courses = courseService.searchCourses(keyword);
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get available categories", description = "Retrieve a list of all available course categories")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    public ResponseEntity<?> getCategories() {
        List<String> categories = courseService.getAvailableCategories();
        return ResponseEntity.ok(Map.of("success", true, "data", categories));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get courses by category", description = "Retrieve published courses filtered by category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<?> getCoursesByCategory(@PathVariable String category) {
        List<Course> courses = courseService.getCoursesByCategory(category);
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "Get courses by level", description = "Retrieve courses filtered by difficulty level")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<?> getCoursesByLevel(@PathVariable String level) {
        List<Course> courses = courseService.getCoursesByLevel(level);
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    @GetMapping("/sorted/newest")
    @Operation(summary = "Get courses sorted by newest", description = "Retrieve published courses sorted by creation date (newest first)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<?> getCoursesSortedByNewest() {
        List<Course> courses = courseService.getCoursesSortedByNewest();
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    @GetMapping("/sorted/rating")
    @Operation(summary = "Get courses sorted by rating", description = "Retrieve published courses sorted by rating (highest first)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<?> getCoursesSortedByRating() {
        List<Course> courses = courseService.getCoursesSortedByRating();
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    @GetMapping("/sorted/popular")
    @Operation(summary = "Get courses sorted by popularity", description = "Retrieve published courses sorted by enrollment count (most popular first)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully")
    })
    public ResponseEntity<?> getCoursesSortedByPopularity() {
        List<Course> courses = courseService.getCoursesSortedByPopularity();
        return ResponseEntity.ok(Map.of("success", true, "data", courses));
    }

    // ========== INSTRUCTOR ENDPOINTS ==========

    @GetMapping("/instructor")
    @Operation(summary = "Get instructor's courses", description = "Retrieve all courses created by the authenticated instructor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getInstructorCourses(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            List<Course> courses = courseService.getCoursesByInstructor(instructorId);
            return ResponseEntity.ok(Map.of("success", true, "data", courses));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new course", description = "Create a new course draft for the authenticated instructor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid course data"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> createCourse(@RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CourseCreateRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Course courseData = new Course();
            courseData.setTitle(request.getTitle());
            courseData.setDescription(request.getDescription());
            courseData.setCategory(request.getCategory());
            courseData.setLevel(request.getLevel());
            courseData.setPrice(request.getPrice());
            courseData.setThumbnailUrl(request.getThumbnailUrl());
            courseData.setDurationHours(request.getDurationHours());
            courseData.setLanguage(request.getLanguage());

            Course createdCourse = courseService.createCourse(courseData, instructorId);
            return ResponseEntity.status(201).body(Map.of(
                    "success", true,
                    "message", "Course created successfully",
                    "data", createdCourse));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("INSTRUCTOR_NOT_FOUND:")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "INSTRUCTOR_NOT_FOUND",
                        "message", "Instructor account not found or inactive"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", errorMessage));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @PutMapping("/{courseId}")
    @Operation(summary = "Update a course", description = "Update an existing course owned by the authenticated instructor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course updated successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to update this course"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateCourse(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String courseId,
            @Valid @RequestBody CourseUpdateRequest request) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Course courseData = new Course();
            courseData.setTitle(request.getTitle());
            courseData.setDescription(request.getDescription());
            courseData.setCategory(request.getCategory());
            courseData.setLevel(request.getLevel());
            courseData.setPrice(request.getPrice());
            courseData.setThumbnailUrl(request.getThumbnailUrl());
            courseData.setDurationHours(request.getDurationHours());
            courseData.setLanguage(request.getLanguage());

            Course updatedCourse = courseService.updateCourse(courseId, courseData, instructorId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course updated successfully",
                    "data", updatedCourse));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "COURSE_NOT_FOUND",
                        "message", "Course not found"));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "You can only update your own courses"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", errorMessage));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @DeleteMapping("/{courseId}")
    @Operation(summary = "Delete a course", description = "Delete a course owned by the authenticated instructor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to delete this course"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> deleteCourse(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            boolean deleted = courseService.deleteCourse(courseId, instructorId);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Course deleted successfully"));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "COURSE_NOT_FOUND",
                        "message", "Course not found"));
            }
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "You can only delete your own courses"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @PutMapping("/{courseId}/publish")
    @Operation(summary = "Publish a course", description = "Publish a course to make it available to students")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course published successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to publish this course"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> publishCourse(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            Course publishedCourse = courseService.publishCourse(courseId, instructorId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course published successfully",
                    "data", publishedCourse));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "COURSE_NOT_FOUND",
                        "message", "Course not found"));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "You can only publish your own courses"));
            } else if (errorMessage.startsWith("VALIDATION_ERROR:")) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "VALIDATION_ERROR",
                        "message", errorMessage.substring("VALIDATION_ERROR:".length())));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", errorMessage));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @PutMapping("/{courseId}/unpublish")
    @Operation(summary = "Unpublish a course", description = "Unpublish a course to hide it from students")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course unpublished successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to unpublish this course"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> unpublishCourse(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            Course unpublishedCourse = courseService.unpublishCourse(courseId, instructorId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course unpublished successfully",
                    "data", unpublishedCourse));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "COURSE_NOT_FOUND",
                        "message", "Course not found"));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "You can only unpublish your own courses"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", errorMessage));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @PostMapping("/{courseId}/thumbnail")
    @Operation(summary = "Upload course thumbnail", description = "Upload a thumbnail image for a course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Thumbnail uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> uploadCourseThumbnail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String courseId,
            @RequestParam("thumbnail") MultipartFile file) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            String thumbnailUrl = courseService.uploadCourseThumbnail(courseId, file, instructorId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course thumbnail uploaded successfully",
                    "data", Map.of("thumbnailUrl", thumbnailUrl)));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "COURSE_NOT_FOUND",
                        "message", "Course not found"));
            } else if (errorMessage.startsWith("UNAUTHORIZED:")) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "UNAUTHORIZED",
                        "message", "You can only update your own courses"));
            }
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "VALIDATION_ERROR",
                    "message", errorMessage));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get instructor statistics", description = "Get course statistics for the authenticated instructor")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getInstructorStatistics(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String instructorId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
            Map<String, Object> statistics = courseService.getCourseStatistics(instructorId);
            return ResponseEntity.ok(Map.of("success", true, "data", statistics));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/admin/all")
    @Operation(summary = "Get all courses (Admin)", description = "Retrieve all courses including drafts (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getAllCoursesAdmin(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "AUTHENTICATION_REQUIRED",
                    "message", "Authentication token is required"));
        }

        try {
            String role = jwtUtil.extractRole(token.replace("Bearer ", ""));
            if (!"admin".equals(role)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "error", "ADMIN_ACCESS_REQUIRED",
                        "message", "Admin access required"));
            }

            List<Course> courses = courseService.getAllCourses();
            return ResponseEntity.ok(Map.of("success", true, "data", courses));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "INVALID_TOKEN",
                    "message", "Invalid authentication token"));
        }
    }
}
