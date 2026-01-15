package com.course_learning.backend.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    private String courseId;
    private String title;
    private String description;
    private String instructorId;
    private String category;
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED
    private Double price;
    private String thumbnailUrl;
    private Integer durationHours;
    private String language;
    private Boolean isPublished = false;
    private Integer enrolledStudents = 0;
    private Integer maxCapacity;
    private Double rating = 0.0;
    private Integer totalReviews = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Course() {
    }

    public Course(String title, String description, String instructorId, String category,
                  String level, Double price, String thumbnailUrl, Integer durationHours, String language) {
        this.title = title;
        this.description = description;
        this.instructorId = instructorId;
        this.category = category;
        this.level = level;
        this.price = price;
        this.thumbnailUrl = thumbnailUrl;
        this.durationHours = durationHours;
        this.language = language;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void generateCourseId() {
        if (this.courseId == null) {
            this.courseId = "CRS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }
    }

    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public Integer getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(Integer enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
