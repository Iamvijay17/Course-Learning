package com.course_learning.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.config.JwtUtil;
import com.course_learning.backend.model.Course;
import com.course_learning.backend.model.Enrollment;
import com.course_learning.backend.service.CourseService;
import com.course_learning.backend.service.EnrollmentService;
import com.course_learning.backend.service.ProgressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "Course Enrollment", description = "APIs for managing course enrollments, including enrollment, cancellation, and completion")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========== STUDENT ENDPOINTS ==========

    @PostMapping("/courses/{courseId}")
    @Operation(summary = "Enroll in a course", description = "Enroll the authenticated user in a published course with availability checking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Enrollment successful"),
        @ApiResponse(responseCode = "400", description = "Enrollment failed - course full, already enrolled, or validation error"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> enrollInCourse(@RequestHeader(value = "Authorization", required = false) String token,
                                           @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Enrollment enrollment = enrollmentService.enrollUserInCourse(userId, courseId);
            return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Successfully enrolled in course",
                "data", Map.of(
                    "enrollmentId", enrollment.getEnrollmentId(),
                    "courseId", enrollment.getCourse().getCourseId(),
                    "courseTitle", enrollment.getCourse().getTitle(),
                    "status", enrollment.getStatus(),
                    "enrolledAt", enrollment.getEnrolledAt()
                )
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("USER_NOT_FOUND:")) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "USER_NOT_FOUND",
                    "message", "User account not found or inactive"
                ));
            } else if (errorMessage.startsWith("COURSE_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found or not published"
                ));
            } else if (errorMessage.startsWith("ALREADY_ENROLLED:")) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "ALREADY_ENROLLED",
                    "message", "You are already enrolled in this course"
                ));
            } else if (errorMessage.startsWith("COURSE_FULL:")) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "COURSE_FULL",
                    "message", "Course has reached maximum capacity"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "ENROLLMENT_FAILED",
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

    @DeleteMapping("/courses/{courseId}")
    @Operation(summary = "Cancel enrollment", description = "Cancel the authenticated user's enrollment in a course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Enrollment cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cancellation failed"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> cancelEnrollment(@RequestHeader(value = "Authorization", required = false) String token,
                                             @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Enrollment enrollment = enrollmentService.cancelEnrollment(userId, courseId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Enrollment cancelled successfully",
                "data", Map.of(
                    "enrollmentId", enrollment.getEnrollmentId(),
                    "courseId", enrollment.getCourse().getCourseId(),
                    "status", enrollment.getStatus(),
                    "cancelledAt", enrollment.getCancelledAt()
                )
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("ENROLLMENT_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "ENROLLMENT_NOT_FOUND",
                    "message", "Enrollment not found"
                ));
            } else if (errorMessage.startsWith("ENROLLMENT_NOT_ACTIVE:")) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "ENROLLMENT_NOT_ACTIVE",
                    "message", "Enrollment is not active"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "CANCELLATION_FAILED",
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

    @PutMapping("/courses/{courseId}/complete")
    @Operation(summary = "Complete enrollment", description = "Mark the authenticated user's enrollment in a course as completed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Enrollment completed successfully"),
        @ApiResponse(responseCode = "400", description = "Completion failed"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> completeEnrollment(@RequestHeader(value = "Authorization", required = false) String token,
                                               @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Enrollment enrollment = enrollmentService.completeEnrollment(userId, courseId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Enrollment completed successfully",
                "data", Map.of(
                    "enrollmentId", enrollment.getEnrollmentId(),
                    "courseId", enrollment.getCourse().getCourseId(),
                    "status", enrollment.getStatus(),
                    "completedAt", enrollment.getCompletedAt()
                )
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("ENROLLMENT_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "ENROLLMENT_NOT_FOUND",
                    "message", "Enrollment not found"
                ));
            } else if (errorMessage.startsWith("ENROLLMENT_NOT_ACTIVE:")) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "ENROLLMENT_NOT_ACTIVE",
                    "message", "Enrollment is not active"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "COMPLETION_FAILED",
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

    @GetMapping("/my-enrollments")
    @Operation(summary = "Get my enrollments", description = "Retrieve all enrollments for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getMyEnrollments(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByUser(userId);
            List<Map<String, Object>> enrollmentData = enrollments.stream()
                .map(enrollment -> Map.<String, Object>of(
                    "enrollmentId", enrollment.getEnrollmentId(),
                    "courseId", enrollment.getCourse().getCourseId(),
                    "courseTitle", enrollment.getCourse().getTitle(),
                    "courseInstructor", enrollment.getCourse().getInstructorId(),
                    "status", enrollment.getStatus(),
                    "enrolledAt", enrollment.getEnrolledAt(),
                    "completedAt", enrollment.getCompletedAt(),
                    "cancelledAt", enrollment.getCancelledAt()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "data", enrollmentData));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @GetMapping("/courses/{courseId}/availability")
    @Operation(summary = "Check course availability", description = "Check if a course is available for enrollment and get availability details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability checked successfully"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<?> checkCourseAvailability(@PathVariable String courseId) {
        try {
            Course course = courseService.getCourseById(courseId);
            if (course == null || !course.getIsPublished()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found or not published"
                ));
            }

            boolean isAvailable = enrollmentService.isCourseAvailable(course);
            int availableSpots = enrollmentService.getAvailableSpots(course);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "courseId", course.getCourseId(),
                    "courseTitle", course.getTitle(),
                    "isAvailable", isAvailable,
                    "maxCapacity", course.getMaxCapacity(),
                    "currentEnrollments", course.getEnrolledStudents(),
                    "availableSpots", availableSpots
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "INTERNAL_ERROR",
                "message", "Failed to check course availability"
            ));
        }
    }

    @GetMapping("/courses/{courseId}/status")
    @Operation(summary = "Get enrollment status", description = "Get the authenticated user's enrollment status for a specific course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Enrollment status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getEnrollmentStatus(@RequestHeader(value = "Authorization", required = false) String token,
                                                @PathVariable String courseId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            Course course = courseService.getCourseById(courseId);
            if (course == null || !course.getIsPublished()) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "COURSE_NOT_FOUND",
                    "message", "Course not found or not published"
                ));
            }

            boolean isEnrolled = enrollmentService.isUserEnrolled(userId, courseId);
            String status = "NOT_ENROLLED";

            if (isEnrolled) {
                Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, courseId).orElse(null);
                if (enrollment != null) {
                    status = enrollment.getStatus();
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "courseId", courseId,
                    "isEnrolled", isEnrolled,
                    "status", status
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    // ========== PROGRESS TRACKING ENDPOINTS ==========

    @PostMapping("/{enrollmentId}/lessons/{lessonId}/start")
    @Operation(summary = "Start a lesson", description = "Mark a lesson as started for progress tracking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lesson started successfully"),
        @ApiResponse(responseCode = "400", description = "Failed to start lesson"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Enrollment or lesson not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> startLesson(@RequestHeader(value = "Authorization", required = false) String token,
                                        @PathVariable String enrollmentId,
                                        @PathVariable String lessonId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            // Verify the enrollment belongs to the user
            Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, null).orElse(null);
            if (enrollment == null || !enrollment.getEnrollmentId().equals(enrollmentId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own enrollments"
                ));
            }

            var progress = progressService.startLesson(enrollmentId, lessonId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lesson started successfully",
                "data", Map.of(
                    "progressId", progress.getProgressId(),
                    "lessonId", progress.getLesson().getLessonId(),
                    "lessonTitle", progress.getLesson().getTitle(),
                    "isCompleted", progress.getIsCompleted(),
                    "lastAccessedAt", progress.getLastAccessedAt()
                )
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("ENROLLMENT_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "ENROLLMENT_NOT_FOUND",
                    "message", "Enrollment not found"
                ));
            } else if (errorMessage.startsWith("LESSON_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "LESSON_NOT_FOUND",
                    "message", "Lesson not found"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "START_LESSON_FAILED",
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

    @PutMapping("/{enrollmentId}/lessons/{lessonId}/watch-time")
    @Operation(summary = "Update watch time", description = "Update the watch time for a lesson")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Watch time updated successfully"),
        @ApiResponse(responseCode = "400", description = "Failed to update watch time"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Progress record not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> updateWatchTime(@RequestHeader(value = "Authorization", required = false) String token,
                                            @PathVariable String enrollmentId,
                                            @PathVariable String lessonId,
                                            @org.springframework.web.bind.annotation.RequestParam int seconds) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            // Verify the enrollment belongs to the user
            Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, null).orElse(null);
            if (enrollment == null || !enrollment.getEnrollmentId().equals(enrollmentId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own enrollments"
                ));
            }

            var progress = progressService.updateWatchTime(enrollmentId, lessonId, seconds);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Watch time updated successfully",
                "data", Map.of(
                    "progressId", progress.getProgressId(),
                    "lessonId", progress.getLesson().getLessonId(),
                    "watchTimeSeconds", progress.getWatchTimeSeconds(),
                    "progressPercentage", Math.round(progress.getProgressPercentage() * 100.0) / 100.0,
                    "lastAccessedAt", progress.getLastAccessedAt()
                )
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("PROGRESS_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "PROGRESS_NOT_FOUND",
                    "message", "Progress record not found"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "UPDATE_WATCH_TIME_FAILED",
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

    @PutMapping("/{enrollmentId}/lessons/{lessonId}/complete")
    @Operation(summary = "Mark lesson as completed", description = "Mark a lesson as completed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lesson marked as completed"),
        @ApiResponse(responseCode = "400", description = "Failed to complete lesson"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Progress record not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> completeLesson(@RequestHeader(value = "Authorization", required = false) String token,
                                           @PathVariable String enrollmentId,
                                           @PathVariable String lessonId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            // Verify the enrollment belongs to the user
            Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, null).orElse(null);
            if (enrollment == null || !enrollment.getEnrollmentId().equals(enrollmentId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own enrollments"
                ));
            }

            var progress = progressService.markLessonCompleted(enrollmentId, lessonId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lesson marked as completed",
                "data", Map.of(
                    "progressId", progress.getProgressId(),
                    "lessonId", progress.getLesson().getLessonId(),
                    "lessonTitle", progress.getLesson().getTitle(),
                    "isCompleted", progress.getIsCompleted(),
                    "completedAt", progress.getCompletedAt()
                )
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.startsWith("PROGRESS_NOT_FOUND:")) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "PROGRESS_NOT_FOUND",
                    "message", "Progress record not found"
                ));
            }
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "error", "COMPLETE_LESSON_FAILED",
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

    @GetMapping("/{enrollmentId}/progress")
    @Operation(summary = "Get enrollment progress", description = "Get detailed progress for an enrollment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Progress retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getEnrollmentProgress(@RequestHeader(value = "Authorization", required = false) String token,
                                                  @PathVariable String enrollmentId) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            // Verify the enrollment belongs to the user
            Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, null).orElse(null);
            if (enrollment == null || !enrollment.getEnrollmentId().equals(enrollmentId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own enrollments"
                ));
            }

            var progressSummary = progressService.getEnrollmentProgressSummary(enrollmentId);
            var detailedProgress = progressService.getDetailedProgress(enrollmentId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "enrollmentId", enrollmentId,
                    "summary", progressSummary,
                    "lessons", detailedProgress
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "INVALID_TOKEN",
                "message", "Invalid authentication token"
            ));
        }
    }

    @GetMapping("/{enrollmentId}/next-lessons")
    @Operation(summary = "Get next lessons", description = "Get the next incomplete lessons for an enrollment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Next lessons retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getNextLessons(@RequestHeader(value = "Authorization", required = false) String token,
                                           @PathVariable String enrollmentId,
                                           @org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") int limit) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_REQUIRED",
                "message", "Authentication token is required"
            ));
        }

        try {
            String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));

            // Verify the enrollment belongs to the user
            Enrollment enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, null).orElse(null);
            if (enrollment == null || !enrollment.getEnrollmentId().equals(enrollmentId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "UNAUTHORIZED",
                    "message", "You can only access your own enrollments"
                ));
            }

            var nextLessons = progressService.getNextLessons(enrollmentId, limit);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", nextLessons
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
