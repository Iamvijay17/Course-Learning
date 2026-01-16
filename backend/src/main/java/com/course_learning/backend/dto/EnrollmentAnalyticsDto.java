package com.course_learning.backend.dto;

import java.time.LocalDate;
import java.util.Map;

public class EnrollmentAnalyticsDto {
    private long totalEnrollments;
    private long activeEnrollments;
    private long completedEnrollments;
    private long cancelledEnrollments;
    private double completionRate;
    private Map<String, Long> enrollmentsByStatus;
    private Map<LocalDate, Long> dailyEnrollments;
    private Map<String, Long> enrollmentsByCourse;
    private Map<String, Double> completionRatesByCourse;

    public EnrollmentAnalyticsDto() {
    }

    public EnrollmentAnalyticsDto(long totalEnrollments, long activeEnrollments, long completedEnrollments,
                                long cancelledEnrollments, double completionRate) {
        this.totalEnrollments = totalEnrollments;
        this.activeEnrollments = activeEnrollments;
        this.completedEnrollments = completedEnrollments;
        this.cancelledEnrollments = cancelledEnrollments;
        this.completionRate = completionRate;
    }

    // Getters and Setters
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

    public Map<String, Long> getEnrollmentsByStatus() {
        return enrollmentsByStatus;
    }

    public void setEnrollmentsByStatus(Map<String, Long> enrollmentsByStatus) {
        this.enrollmentsByStatus = enrollmentsByStatus;
    }

    public Map<LocalDate, Long> getDailyEnrollments() {
        return dailyEnrollments;
    }

    public void setDailyEnrollments(Map<LocalDate, Long> dailyEnrollments) {
        this.dailyEnrollments = dailyEnrollments;
    }

    public Map<String, Long> getEnrollmentsByCourse() {
        return enrollmentsByCourse;
    }

    public void setEnrollmentsByCourse(Map<String, Long> enrollmentsByCourse) {
        this.enrollmentsByCourse = enrollmentsByCourse;
    }

    public Map<String, Double> getCompletionRatesByCourse() {
        return completionRatesByCourse;
    }

    public void setCompletionRatesByCourse(Map<String, Double> completionRatesByCourse) {
        this.completionRatesByCourse = completionRatesByCourse;
    }
}
