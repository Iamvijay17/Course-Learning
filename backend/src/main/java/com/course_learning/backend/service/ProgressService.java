package com.course_learning.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.course_learning.backend.model.Enrollment;
import com.course_learning.backend.model.Lesson;
import com.course_learning.backend.model.Progress;
import com.course_learning.backend.repository.EnrollmentRepository;
import com.course_learning.backend.repository.LessonRepository;
import com.course_learning.backend.repository.ProgressRepository;

@Service
public class ProgressService {

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    public List<Progress> getProgressByEnrollment(String enrollmentId) {
        return progressRepository.findByEnrollmentIdOrderByLessonOrder(enrollmentId);
    }

    public List<Progress> getProgressByUserAndCourse(String userId, String courseId) {
        return progressRepository.findByEnrollment_User_UserIdAndEnrollment_Course_CourseId(userId, courseId);
    }

    public Optional<Progress> getProgressByEnrollmentAndLesson(String enrollmentId, String lessonId) {
        return progressRepository.findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId);
    }

    @Transactional
    public Progress startLesson(String enrollmentId, String lessonId) {
        // Validate enrollment exists and is active
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
        if (!enrollmentOpt.isPresent() || !"ACTIVE".equals(enrollmentOpt.get().getStatus())) {
            throw new RuntimeException("ENROLLMENT_NOT_FOUND:Enrollment not found or not active");
        }

        // Validate lesson exists
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (!lessonOpt.isPresent()) {
            throw new RuntimeException("LESSON_NOT_FOUND:Lesson not found");
        }

        Lesson lesson = lessonOpt.get();
        Enrollment enrollment = enrollmentOpt.get();

        // Check if progress already exists
        Optional<Progress> existingProgress = progressRepository.findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId);
        if (existingProgress.isPresent()) {
            Progress progress = existingProgress.get();
            progress.setLastAccessedAt(LocalDateTime.now());
            return progressRepository.save(progress);
        }

        // Create new progress record
        Progress progress = new Progress(enrollment, lesson);
        return progressRepository.save(progress);
    }

    @Transactional
    public Progress updateWatchTime(String enrollmentId, String lessonId, int watchTimeSeconds) {
        Optional<Progress> progressOpt = progressRepository.findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId);
        if (!progressOpt.isPresent()) {
            throw new RuntimeException("PROGRESS_NOT_FOUND:Progress record not found");
        }

        Progress progress = progressOpt.get();
        progress.updateWatchTime(watchTimeSeconds);
        return progressRepository.save(progress);
    }

    @Transactional
    public Progress markLessonCompleted(String enrollmentId, String lessonId) {
        Optional<Progress> progressOpt = progressRepository.findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId);
        if (!progressOpt.isPresent()) {
            throw new RuntimeException("PROGRESS_NOT_FOUND:Progress record not found");
        }

        Progress progress = progressOpt.get();
        progress.markAsCompleted();
        return progressRepository.save(progress);
    }

    @Transactional
    public Progress markLessonIncomplete(String enrollmentId, String lessonId) {
        Optional<Progress> progressOpt = progressRepository.findByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lessonId);
        if (!progressOpt.isPresent()) {
            throw new RuntimeException("PROGRESS_NOT_FOUND:Progress record not found");
        }

        Progress progress = progressOpt.get();
        progress.setIsCompleted(false);
        progress.setCompletedAt(null);
        return progressRepository.save(progress);
    }

    public Map<String, Object> getEnrollmentProgressSummary(String enrollmentId) {
        long totalLessons = progressRepository.countTotalLessonsByEnrollmentId(enrollmentId);
        long completedLessons = progressRepository.countCompletedLessonsByEnrollmentId(enrollmentId);
        Long totalWatchTime = progressRepository.getTotalWatchTimeByEnrollmentId(enrollmentId);

        double completionPercentage = totalLessons > 0 ? (double) completedLessons / totalLessons * 100 : 0.0;

        return Map.of(
            "enrollmentId", enrollmentId,
            "totalLessons", totalLessons,
            "completedLessons", completedLessons,
            "completionPercentage", Math.round(completionPercentage * 100.0) / 100.0,
            "totalWatchTimeSeconds", totalWatchTime != null ? totalWatchTime : 0,
            "isCompleted", completionPercentage >= 100.0
        );
    }

    public List<Map<String, Object>> getDetailedProgress(String enrollmentId) {
        List<Progress> progressList = progressRepository.findByEnrollmentIdOrderByLessonOrder(enrollmentId);

        return progressList.stream()
            .map(progress -> Map.<String, Object>of(
                "progressId", progress.getProgressId(),
                "lessonId", progress.getLesson().getLessonId(),
                "lessonTitle", progress.getLesson().getTitle(),
                "lessonOrder", progress.getLesson().getOrderIndex(),
                "isCompleted", progress.getIsCompleted(),
                "progressPercentage", Math.round(progress.getProgressPercentage() * 100.0) / 100.0,
                "watchTimeSeconds", progress.getWatchTimeSeconds(),
                "totalWatchTimeSeconds", progress.getTotalWatchTimeSeconds(),
                "completedAt", progress.getCompletedAt(),
                "lastAccessedAt", progress.getLastAccessedAt()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public void initializeProgressForEnrollment(String enrollmentId) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);
        if (!enrollmentOpt.isPresent()) {
            throw new RuntimeException("ENROLLMENT_NOT_FOUND:Enrollment not found");
        }

        Enrollment enrollment = enrollmentOpt.get();
        String courseId = enrollment.getCourse().getCourseId();

        // Get all lessons for the course
        List<Lesson> lessons = lessonRepository.findLessonsByCourseId(courseId);

        // Create progress records for lessons that don't have them yet
        for (Lesson lesson : lessons) {
            if (!progressRepository.existsByEnrollment_EnrollmentIdAndLesson_LessonId(enrollmentId, lesson.getLessonId())) {
                Progress progress = new Progress(enrollment, lesson);
                progressRepository.save(progress);
            }
        }
    }

    public boolean isLessonAccessible(String userId, String lessonId) {
        // Check if user is enrolled in the course containing this lesson
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (!lessonOpt.isPresent()) {
            return false;
        }

        String courseId = lessonOpt.get().getModule().getSection().getCourseId();
        return enrollmentRepository.existsByUser_UserIdAndCourse_CourseIdAndStatus(userId, courseId, "ACTIVE");
    }

    public List<Map<String, Object>> getNextLessons(String enrollmentId, int limit) {
        List<Progress> progressList = progressRepository.findByEnrollmentIdOrderByLessonOrder(enrollmentId);

        return progressList.stream()
            .filter(progress -> !progress.getIsCompleted())
            .limit(limit)
            .map(progress -> Map.<String, Object>of(
                "lessonId", progress.getLesson().getLessonId(),
                "lessonTitle", progress.getLesson().getTitle(),
                "moduleTitle", progress.getLesson().getModule().getTitle(),
                "progressPercentage", Math.round(progress.getProgressPercentage() * 100.0) / 100.0,
                "lastAccessedAt", progress.getLastAccessedAt()
            ))
            .collect(Collectors.toList());
    }
}
