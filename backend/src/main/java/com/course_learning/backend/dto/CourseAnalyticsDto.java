package com.course_learning.backend.dto;

import java.time.LocalDate;
import java.util.Map;

public class CourseAnalyticsDto {
    private String courseId;
    private String courseTitle;
    private long totalEnrollments;
    private long activeEnrollments;
    private long completedEnrollments;
    private long cancelledEnrollments;
    private double completionRate;
    private double averageRating;
    private long totalReviews;
    private Map<LocalDate, Long> enrollmentsOverTime;
    private Map<String, Long> enrollmentsByUserRole;

    public CourseAnalyticsDto() {
    }

    public CourseAnalyticsDto(String courseId, String courseTitle, long totalEnrollments,
                            long activeEnrollments, long completedEnrollments, long cancelledEnrollments,
                            double completionRate) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalEnrollments = totalEnrollments;
        this.activeEnrollments = activeEnrollments;
        this.completedEnrollments = completedEnrollments;
        this.cancelledEnrollments = cancelledEnrollments;
        this.completionRate = completionRate;
    }

    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public long getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(long totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public long getActiveEnrollments() {
        return activeEnrollments;
    }

    public void setActiveEnrollments(long activeEnrollments) {
        this.activeEnrollments = activeEnrollments;
    }

    public long getCompletedEnrollments() {
        return completedEnrollments;
    }

    public void setCompletedEnrollments(long completedEnrollments) {
        this.completedEnrollments = completedEnrollments;
    }

    public long getCancelledEnrollments() {
        return cancelledEnrollments;
    }

    public void setCancelledEnrollments(long cancelledEnrollments) {
        this.cancelledEnrollments = cancelledEnrollments;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Map<LocalDate, Long> getEnrollmentsOverTime() {
        return enrollmentsOverTime;
    }

    public void setEnrollmentsOverTime(Map<LocalDate, Long> enrollmentsOverTime) {
        this.enrollmentsOverTime = enrollmentsOverTime;
    }

    public Map<String, Long> getEnrollmentsByUserRole() {
        return enrollmentsByUserRole;
    }

    public void setEnrollmentsByUserRole(Map<String, Long> enrollmentsByUserRole) {
        this.enrollmentsByUserRole = enrollmentsByUserRole;
    }
}
