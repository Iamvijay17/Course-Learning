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
@Table(name = "progress")
public class Progress {
    @Id
    private String progressId;

    @ManyToOne
    @JoinColumn(name = "enrollmentId", nullable = false)
    private Enrollment enrollment;

    @ManyToOne
    @JoinColumn(name = "lessonId", nullable = false)
    private Lesson lesson;

    private Boolean isCompleted = false;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
    private Integer watchTimeSeconds = 0; // Time spent watching the video
    private Integer totalWatchTimeSeconds; // Total duration of the lesson

    public Progress() {
    }

    public Progress(Enrollment enrollment, Lesson lesson) {
        this.enrollment = enrollment;
        this.lesson = lesson;
        this.lastAccessedAt = LocalDateTime.now();
        // Parse video duration to seconds if available
        if (lesson.getVideoDuration() != null) {
            this.totalWatchTimeSeconds = parseDurationToSeconds(lesson.getVideoDuration());
        }
    }

    @PrePersist
    public void generateProgressId() {
        if (this.progressId == null) {
            this.progressId = "PRG" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
    }

    private Integer parseDurationToSeconds(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            return 0;
        }
        try {
            String[] parts = duration.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            } else if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return minutes * 60 + seconds;
            }
        } catch (NumberFormatException e) {
            // Invalid format, return 0
        }
        return 0;
    }

    // Getters and Setters
    public String getProgressId() {
        return progressId;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
        if (isCompleted && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Integer getWatchTimeSeconds() {
        return watchTimeSeconds;
    }

    public void setWatchTimeSeconds(Integer watchTimeSeconds) {
        this.watchTimeSeconds = watchTimeSeconds;
    }

    public Integer getTotalWatchTimeSeconds() {
        return totalWatchTimeSeconds;
    }

    public void setTotalWatchTimeSeconds(Integer totalWatchTimeSeconds) {
        this.totalWatchTimeSeconds = totalWatchTimeSeconds;
    }

    // Helper methods
    public double getProgressPercentage() {
        if (totalWatchTimeSeconds == null || totalWatchTimeSeconds == 0) {
            return isCompleted ? 100.0 : 0.0;
        }
        return Math.min(100.0, (watchTimeSeconds.doubleValue() / totalWatchTimeSeconds.doubleValue()) * 100.0);
    }

    public void markAsCompleted() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        if (totalWatchTimeSeconds != null) {
            this.watchTimeSeconds = totalWatchTimeSeconds;
        }
    }

    public void updateWatchTime(int additionalSeconds) {
        this.watchTimeSeconds = Math.max(0, this.watchTimeSeconds + additionalSeconds);
        this.lastAccessedAt = LocalDateTime.now();
    }
}
