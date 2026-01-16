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

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = 'COMPLETED'")
    long countCompletedEnrollments();

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = 'CANCELLED'")
    long countCancelledEnrollments();

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.status = 'ACTIVE'")
    long countActiveEnrollments();

    @Query("SELECT COUNT(e) FROM Enrollment e")
    long countTotalEnrollments();

    @Query("SELECT DATE(e.enrolledAt) as date, COUNT(e) as count FROM Enrollment e WHERE e.enrolledAt >= :startDate GROUP BY DATE(e.enrolledAt) ORDER BY DATE(e.enrolledAt)")
    List<Object[]> countEnrollmentsByDate(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT e.course.courseId, COUNT(e) FROM Enrollment e GROUP BY e.course.courseId ORDER BY COUNT(e) DESC")
    List<Object[]> countEnrollmentsByCourse();

    @Query("SELECT e.course.courseId, e.status, COUNT(e) FROM Enrollment e GROUP BY e.course.courseId, e.status")
    List<Object[]> countEnrollmentsByCourseAndStatus();

    @Query("SELECT e.course.courseId, COUNT(e) FROM Enrollment e WHERE e.status = 'COMPLETED' GROUP BY e.course.courseId")
    List<Object[]> countCompletedEnrollmentsByCourse();

    @Query("SELECT e.course.courseId, COUNT(e) FROM Enrollment e GROUP BY e.course.courseId")
    List<Object[]> countTotalEnrollmentsByCourse();

    boolean existsByUser_UserIdAndCourse_CourseIdAndStatus(String userId, String courseId, String status);
}
