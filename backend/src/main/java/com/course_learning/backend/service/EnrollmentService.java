package com.course_learning.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.course_learning.backend.model.Course;
import com.course_learning.backend.model.Enrollment;
import com.course_learning.backend.model.User;
import com.course_learning.backend.repository.CourseRepository;
import com.course_learning.backend.repository.EnrollmentRepository;
import com.course_learning.backend.repository.UserRepository;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ProgressService progressService;

    public List<Enrollment> getEnrollmentsByUser(String userId) {
        return enrollmentRepository.findByUser_UserId(userId);
    }

    public List<Enrollment> getEnrollmentsByCourse(String courseId) {
        return enrollmentRepository.findByCourse_CourseId(courseId);
    }

    public List<Enrollment> getActiveEnrollmentsByUser(String userId) {
        return enrollmentRepository.findByUser_UserIdAndStatus(userId, "ACTIVE");
    }

    public Optional<Enrollment> getEnrollmentByUserAndCourse(String userId, String courseId) {
        return enrollmentRepository.findByUser_UserIdAndCourse_CourseId(userId, courseId);
    }

    @Transactional
    public Enrollment enrollUserInCourse(String userId, String courseId) {
        // Validate user exists and is active
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            throw new RuntimeException("USER_NOT_FOUND:User not found or inactive");
        }

        // Validate course exists and is published
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || !course.getIsPublished()) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found or not published");
        }

        // Check if user is already enrolled
        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByUser_UserIdAndCourse_CourseId(userId,
                courseId);
        if (existingEnrollment.isPresent()) {
            Enrollment enrollment = existingEnrollment.get();
            if ("ACTIVE".equals(enrollment.getStatus())) {
                throw new RuntimeException("ALREADY_ENROLLED:User is already enrolled in this course");
            } else if ("CANCELLED".equals(enrollment.getStatus())) {
                // Reactivate cancelled enrollment
                enrollment.setStatus("ACTIVE");
                enrollment.setEnrolledAt(LocalDateTime.now());
                enrollment.setCancelledAt(null);
                return enrollmentRepository.save(enrollment);
            }
        }

        // Check course availability
        if (!isCourseAvailable(course)) {
            throw new RuntimeException("COURSE_FULL:Course has reached maximum capacity");
        }

        // Create new enrollment
        Enrollment enrollment = new Enrollment(user, course);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // Update course enrollment count
        courseService.updateCourseStats(courseId, 0.0, 1);

        return savedEnrollment;
    }

    @Transactional
    public Enrollment cancelEnrollment(String userId, String courseId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUser_UserIdAndCourse_CourseId(userId, courseId);
        if (!enrollmentOpt.isPresent()) {
            throw new RuntimeException("ENROLLMENT_NOT_FOUND:Enrollment not found");
        }

        Enrollment enrollment = enrollmentOpt.get();
        if (!"ACTIVE".equals(enrollment.getStatus())) {
            throw new RuntimeException("ENROLLMENT_NOT_ACTIVE:Enrollment is not active");
        }

        // Cancel enrollment
        enrollment.setStatus("CANCELLED");
        enrollment.setCancelledAt(LocalDateTime.now());
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // Update course enrollment count
        courseService.updateCourseStats(courseId, 0.0, -1);

        return savedEnrollment;
    }

    @Transactional
    public Enrollment completeEnrollment(String userId, String courseId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByUser_UserIdAndCourse_CourseId(userId, courseId);
        if (!enrollmentOpt.isPresent()) {
            throw new RuntimeException("ENROLLMENT_NOT_FOUND:Enrollment not found");
        }

        Enrollment enrollment = enrollmentOpt.get();
        if (!"ACTIVE".equals(enrollment.getStatus())) {
            throw new RuntimeException("ENROLLMENT_NOT_ACTIVE:Enrollment is not active");
        }

        // Check if all lessons are completed before allowing course completion
        Map<String, Object> progressSummary = progressService
                .getEnrollmentProgressSummary(enrollment.getEnrollmentId());
        Boolean isCompleted = (Boolean) progressSummary.get("isCompleted");
        if (!isCompleted) {
            throw new RuntimeException(
                    "COURSE_NOT_COMPLETED:All lessons must be completed before marking the course as completed");
        }

        // Complete enrollment
        enrollment.setStatus("COMPLETED");
        enrollment.setCompletedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    public boolean isCourseAvailable(Course course) {
        if (course.getMaxCapacity() == null) {
            // No capacity limit set, course is always available
            return true;
        }

        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByCourseId(course.getCourseId());
        return activeEnrollments < course.getMaxCapacity();
    }

    public int getAvailableSpots(Course course) {
        if (course.getMaxCapacity() == null) {
            return Integer.MAX_VALUE; // Unlimited
        }

        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByCourseId(course.getCourseId());
        return Math.max(0, course.getMaxCapacity() - (int) activeEnrollments);
    }

    public boolean isUserEnrolled(String userId, String courseId) {
        return enrollmentRepository.existsByUser_UserIdAndCourse_CourseIdAndStatus(userId, courseId, "ACTIVE");
    }
}
