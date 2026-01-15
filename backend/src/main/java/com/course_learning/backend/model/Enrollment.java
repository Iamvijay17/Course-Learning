package com.course_learning.backend.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "enrollments")
public class Enrollment {
    @Id
    private String enrollmentId;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "courseId", nullable = false)
    private Course course;

    private String status; // ACTIVE, COMPLETED, CANCELLED
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    public Enrollment() {
    }

    public Enrollment(User user, Course course) {
        this.user = user;
        this.course = course;
        this.status = "ACTIVE";
        this.enrolledAt = LocalDateTime.now();
    }

    @PrePersist
    public void generateEnrollmentId() {
        if (this.enrollmentId == null) {
            this.enrollmentId = "ENR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
    }

    // Getters and Setters
    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
