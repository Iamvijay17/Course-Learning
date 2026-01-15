package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Enrollment;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    List<Enrollment> findByUser_UserId(String userId);

    List<Enrollment> findByCourse_CourseId(String courseId);

    List<Enrollment> findByUser_UserIdAndStatus(String userId, String status);

    List<Enrollment> findByCourse_CourseIdAndStatus(String courseId, String status);

    Optional<Enrollment> findByUser_UserIdAndCourse_CourseId(String userId, String courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.courseId = :courseId AND e.status = 'ACTIVE'")
    long countActiveEnrollmentsByCourseId(@Param("courseId") String courseId);

    boolean existsByUser_UserIdAndCourse_CourseIdAndStatus(String userId, String courseId, String status);
}
