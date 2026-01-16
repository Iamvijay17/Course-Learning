package com.course_learning.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.course_learning.backend.model.Certificate;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, String> {

    Optional<Certificate> findByEnrollment_EnrollmentId(String enrollmentId);

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    List<Certificate> findByEnrollment_User_UserId(String userId);

    List<Certificate> findByEnrollment_Course_CourseId(String courseId);

    boolean existsByEnrollment_EnrollmentId(String enrollmentId);

    @Query("SELECT c FROM Certificate c WHERE c.enrollment.user.userId = :userId AND c.enrollment.course.courseId = :courseId")
    Optional<Certificate> findByUserIdAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);
}
