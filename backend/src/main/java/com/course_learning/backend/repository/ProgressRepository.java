package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Progress;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, String> {
    List<Progress> findByEnrollment_EnrollmentId(String enrollmentId);

    List<Progress> findByEnrollment_User_UserIdAndEnrollment_Course_CourseId(String userId, String courseId);

    List<Progress> findByLesson_LessonId(String lessonId);

    Optional<Progress> findByEnrollment_EnrollmentIdAndLesson_LessonId(String enrollmentId, String lessonId);

    boolean existsByEnrollment_EnrollmentIdAndLesson_LessonId(String enrollmentId, String lessonId);

    @Query("SELECT COUNT(p) FROM Progress p WHERE p.enrollment.enrollmentId = :enrollmentId AND p.isCompleted = true")
    long countCompletedLessonsByEnrollmentId(@Param("enrollmentId") String enrollmentId);

    @Query("SELECT COUNT(p) FROM Progress p WHERE p.enrollment.enrollmentId = :enrollmentId")
    long countTotalLessonsByEnrollmentId(@Param("enrollmentId") String enrollmentId);

    @Query("SELECT SUM(p.watchTimeSeconds) FROM Progress p WHERE p.enrollment.enrollmentId = :enrollmentId")
    Long getTotalWatchTimeByEnrollmentId(@Param("enrollmentId") String enrollmentId);

    @Query("SELECT p FROM Progress p WHERE p.enrollment.enrollmentId = :enrollmentId ORDER BY p.lesson.orderIndex ASC")
    List<Progress> findByEnrollmentIdOrderByLessonOrder(@Param("enrollmentId") String enrollmentId);
}
