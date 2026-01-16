package com.course_learning.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.course_learning.backend.dto.CourseAnalyticsDto;
import com.course_learning.backend.dto.EnrollmentAnalyticsDto;
import com.course_learning.backend.model.Course;
import com.course_learning.backend.repository.CourseRepository;
import com.course_learning.backend.repository.EnrollmentRepository;

@Service
public class EnrollmentAnalyticsService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    public EnrollmentAnalyticsDto getOverallAnalytics() {
        EnrollmentAnalyticsDto analytics = new EnrollmentAnalyticsDto();

        // Basic counts
        long totalEnrollments = enrollmentRepository.countTotalEnrollments();
        long activeEnrollments = enrollmentRepository.countActiveEnrollments();
        long completedEnrollments = enrollmentRepository.countCompletedEnrollments();
        long cancelledEnrollments = enrollmentRepository.countCancelledEnrollments();

        analytics.setTotalEnrollments(totalEnrollments);
        analytics.setActiveEnrollments(activeEnrollments);
        analytics.setCompletedEnrollments(completedEnrollments);
        analytics.setCancelledEnrollments(cancelledEnrollments);

        // Completion rate
        double completionRate = totalEnrollments > 0 ? (double) completedEnrollments / totalEnrollments * 100 : 0.0;
        analytics.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);

        // Enrollments by status
        Map<String, Long> enrollmentsByStatus = new HashMap<>();
        enrollmentsByStatus.put("ACTIVE", activeEnrollments);
        enrollmentsByStatus.put("COMPLETED", completedEnrollments);
        enrollmentsByStatus.put("CANCELLED", cancelledEnrollments);
        analytics.setEnrollmentsByStatus(enrollmentsByStatus);

        // Daily enrollments for last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dailyData = enrollmentRepository.countEnrollmentsByDate(thirtyDaysAgo);
        Map<LocalDate, Long> dailyEnrollments = new HashMap<>();
        for (Object[] row : dailyData) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = (Long) row[1];
            dailyEnrollments.put(date, count);
        }
        analytics.setDailyEnrollments(dailyEnrollments);

        // Enrollments by course
        List<Object[]> courseData = enrollmentRepository.countEnrollmentsByCourse();
        Map<String, Long> enrollmentsByCourse = new HashMap<>();
        for (Object[] row : courseData) {
            String courseId = (String) row[0];
            Long count = (Long) row[1];
            enrollmentsByCourse.put(courseId, count);
        }
        analytics.setEnrollmentsByCourse(enrollmentsByCourse);

        // Completion rates by course
        Map<String, Double> completionRatesByCourse = calculateCompletionRatesByCourse();
        analytics.setCompletionRatesByCourse(completionRatesByCourse);

        return analytics;
    }

    public CourseAnalyticsDto getCourseAnalytics(String courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        CourseAnalyticsDto analytics = new CourseAnalyticsDto();
        analytics.setCourseId(courseId);
        analytics.setCourseTitle(course.getTitle());

        // Get enrollment counts by status for this course
        List<Object[]> courseStatusData = enrollmentRepository.countEnrollmentsByCourseAndStatus();
        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : courseStatusData) {
            String cid = (String) row[0];
            String status = (String) row[1];
            Long count = (Long) row[2];
            if (cid.equals(courseId)) {
                statusCounts.put(status, count);
            }
        }

        long totalEnrollments = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long activeEnrollments = statusCounts.getOrDefault("ACTIVE", 0L);
        long completedEnrollments = statusCounts.getOrDefault("COMPLETED", 0L);
        long cancelledEnrollments = statusCounts.getOrDefault("CANCELLED", 0L);

        analytics.setTotalEnrollments(totalEnrollments);
        analytics.setActiveEnrollments(activeEnrollments);
        analytics.setCompletedEnrollments(completedEnrollments);
        analytics.setCancelledEnrollments(cancelledEnrollments);

        // Completion rate
        double completionRate = totalEnrollments > 0 ? (double) completedEnrollments / totalEnrollments * 100 : 0.0;
        analytics.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);

        // Course rating data
        analytics.setAverageRating(course.getRating());
        analytics.setTotalReviews(course.getTotalReviews());

        return analytics;
    }

    public List<CourseAnalyticsDto> getTopCoursesByEnrollments(int limit) {
        List<Object[]> courseData = enrollmentRepository.countEnrollmentsByCourse();

        return courseData.stream()
            .limit(limit)
            .map(row -> {
                String courseId = (String) row[0];
                Long enrollmentCount = (Long) row[1];

                Course course = courseRepository.findById(courseId).orElse(null);
                if (course == null) return null;

                CourseAnalyticsDto dto = new CourseAnalyticsDto();
                dto.setCourseId(courseId);
                dto.setCourseTitle(course.getTitle());
                dto.setTotalEnrollments(enrollmentCount);

                // Calculate completion rate for this course
                Map<String, Double> completionRates = calculateCompletionRatesByCourse();
                dto.setCompletionRate(completionRates.getOrDefault(courseId, 0.0));

                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getEnrollmentTrends(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> trendData = enrollmentRepository.countEnrollmentsByDate(startDate);

        Map<String, Object> trends = new HashMap<>();
        trends.put("period", days + " days");
        trends.put("data", trendData.stream()
            .map(row -> {
                Map<String, Object> point = new HashMap<>();
                LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                Long count = (Long) row[1];
                point.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                point.put("enrollments", count);
                return point;
            })
            .collect(Collectors.toList()));

        return trends;
    }

    private Map<String, Double> calculateCompletionRatesByCourse() {
        List<Object[]> completedData = enrollmentRepository.countCompletedEnrollmentsByCourse();
        List<Object[]> totalData = enrollmentRepository.countTotalEnrollmentsByCourse();

        Map<String, Long> completedByCourse = new HashMap<>();
        Map<String, Long> totalByCourse = new HashMap<>();

        for (Object[] row : completedData) {
            String courseId = (String) row[0];
            Long count = (Long) row[1];
            completedByCourse.put(courseId, count);
        }

        for (Object[] row : totalData) {
            String courseId = (String) row[0];
            Long count = (Long) row[1];
            totalByCourse.put(courseId, count);
        }

        Map<String, Double> completionRates = new HashMap<>();
        for (String courseId : totalByCourse.keySet()) {
            Long total = totalByCourse.get(courseId);
            Long completed = completedByCourse.getOrDefault(courseId, 0L);
            double rate = total > 0 ? (double) completed / total * 100 : 0.0;
            completionRates.put(courseId, Math.round(rate * 100.0) / 100.0);
        }

        return completionRates;
    }
}
