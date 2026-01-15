package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Course;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByInstructorId(String instructorId);

    List<Course> findByCategory(String category);

    List<Course> findByLevel(String level);

    List<Course> findByIsPublishedTrue();

    List<Course> findByIsPublishedFalse();

    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Course> searchPublishedCoursesByTitle(@Param("keyword") String keyword);

    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND LOWER(c.category) LIKE LOWER(CONCAT('%', :category, '%'))")
    List<Course> findPublishedCoursesByCategory(@Param("category") String category);

    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.createdAt DESC")
    List<Course> findPublishedCoursesOrderByCreatedAtDesc();

    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.rating DESC")
    List<Course> findPublishedCoursesOrderByRatingDesc();

    @Query("SELECT c FROM Course c WHERE c.isPublished = true ORDER BY c.enrolledStudents DESC")
    List<Course> findPublishedCoursesOrderByEnrolledStudentsDesc();

    @Query("SELECT DISTINCT c.category FROM Course c WHERE c.isPublished = true")
    List<String> findDistinctCategories();

    @Query("SELECT c FROM Course c WHERE c.instructorId = :instructorId AND c.isPublished = true")
    List<Course> findPublishedCoursesByInstructorId(@Param("instructorId") String instructorId);
}
