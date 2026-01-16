package com.course_learning.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.dto.CourseAnalyticsDto;
import com.course_learning.backend.dto.EnrollmentAnalyticsDto;
import com.course_learning.backend.service.EnrollmentAnalyticsService;

@RestController
@RequestMapping("/api/analytics/enrollments")
public class EnrollmentAnalyticsController {

    @Autowired
    private EnrollmentAnalyticsService analyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<EnrollmentAnalyticsDto> getOverallAnalytics() {
        EnrollmentAnalyticsDto analytics = analyticsService.getOverallAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<CourseAnalyticsDto> getCourseAnalytics(@PathVariable String courseId) {
        try {
            CourseAnalyticsDto analytics = analyticsService.getCourseAnalytics(courseId);
            return ResponseEntity.ok(analytics);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/courses/top")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<CourseAnalyticsDto>> getTopCoursesByEnrollments(
            @RequestParam(defaultValue = "10") int limit) {
        List<CourseAnalyticsDto> topCourses = analyticsService.getTopCoursesByEnrollments(limit);
        return ResponseEntity.ok(topCourses);
    }

    @GetMapping("/trends")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEnrollmentTrends(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> trends = analyticsService.getEnrollmentTrends(days);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/courses/{courseId}/trends")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCourseEnrollmentTrends(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "30") int days) {
        // For now, return general trends - could be enhanced to filter by course
        Map<String, Object> trends = analyticsService.getEnrollmentTrends(days);
        trends.put("courseId", courseId);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        EnrollmentAnalyticsDto overall = analyticsService.getOverallAnalytics();
        List<CourseAnalyticsDto> topCourses = analyticsService.getTopCoursesByEnrollments(5);

        Map<String, Object> summary = Map.of(
            "overall", Map.of(
                "totalEnrollments", overall.getTotalEnrollments(),
                "activeEnrollments", overall.getActiveEnrollments(),
                "completedEnrollments", overall.getCompletedEnrollments(),
                "completionRate", overall.getCompletionRate()
            ),
            "topCourses", topCourses,
            "enrollmentTrends", analyticsService.getEnrollmentTrends(7)
        );

        return ResponseEntity.ok(summary);
    }
}
