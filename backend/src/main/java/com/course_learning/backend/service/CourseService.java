package com.course_learning.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.course_learning.backend.model.Course;
import com.course_learning.backend.model.User;
import com.course_learning.backend.repository.CourseRepository;
import com.course_learning.backend.repository.UserRepository;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> getPublishedCourses() {
        return courseRepository.findByIsPublishedTrue();
    }

    public List<Course> getCoursesByInstructor(String instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    public List<Course> getPublishedCoursesByInstructor(String instructorId) {
        return courseRepository.findPublishedCoursesByInstructorId(instructorId);
    }

    public Course getCourseById(String courseId) {
        return courseRepository.findById(courseId).orElse(null);
    }

    public Course createCourse(Course courseData, String instructorId) {
        // Verify instructor exists and is active
        User instructor = userRepository.findById(instructorId).orElse(null);
        if (instructor == null || !instructor.isActive()) {
            throw new RuntimeException("INSTRUCTOR_NOT_FOUND:Instructor not found or inactive");
        }

        Course course = new Course();
        course.setTitle(courseData.getTitle());
        course.setDescription(courseData.getDescription());
        course.setInstructorId(instructorId);
        course.setCategory(courseData.getCategory());
        course.setLevel(courseData.getLevel());
        course.setPrice(courseData.getPrice());
        course.setThumbnailUrl(courseData.getThumbnailUrl());
        course.setDurationHours(courseData.getDurationHours());
        course.setLanguage(courseData.getLanguage());
        course.setIsPublished(false); // Courses start as drafts
        course.setEnrolledStudents(0);
        course.setRating(0.0);
        course.setTotalReviews(0);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());

        return courseRepository.save(course);
    }

    public Course updateCourse(String courseId, Course courseData, String instructorId) {
        Course existingCourse = courseRepository.findById(courseId).orElse(null);
        if (existingCourse == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        // Verify the instructor owns this course
        if (!existingCourse.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only update your own courses");
        }

        // Update fields
        if (courseData.getTitle() != null) {
            existingCourse.setTitle(courseData.getTitle());
        }
        if (courseData.getDescription() != null) {
            existingCourse.setDescription(courseData.getDescription());
        }
        if (courseData.getCategory() != null) {
            existingCourse.setCategory(courseData.getCategory());
        }
        if (courseData.getLevel() != null) {
            existingCourse.setLevel(courseData.getLevel());
        }
        if (courseData.getPrice() != null) {
            existingCourse.setPrice(courseData.getPrice());
        }
        if (courseData.getThumbnailUrl() != null) {
            existingCourse.setThumbnailUrl(courseData.getThumbnailUrl());
        }
        if (courseData.getDurationHours() != null) {
            existingCourse.setDurationHours(courseData.getDurationHours());
        }
        if (courseData.getLanguage() != null) {
            existingCourse.setLanguage(courseData.getLanguage());
        }

        existingCourse.setUpdatedAt(LocalDateTime.now());
        return courseRepository.save(existingCourse);
    }

    public boolean deleteCourse(String courseId, String instructorId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return false;
        }

        // Verify the instructor owns this course
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only delete your own courses");
        }

        courseRepository.deleteById(courseId);
        return true;
    }

    public Course publishCourse(String courseId, String instructorId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        // Verify the instructor owns this course
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only publish your own courses");
        }

        // Validate required fields before publishing
        if (course.getTitle() == null || course.getTitle().trim().isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR:Course title is required");
        }
        if (course.getDescription() == null || course.getDescription().trim().isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR:Course description is required");
        }
        if (course.getCategory() == null || course.getCategory().trim().isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR:Course category is required");
        }

        course.setIsPublished(true);
        course.setUpdatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    public Course unpublishCourse(String courseId, String instructorId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        // Verify the instructor owns this course
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only unpublish your own courses");
        }

        course.setIsPublished(false);
        course.setUpdatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPublishedCourses();
        }
        return courseRepository.searchPublishedCoursesByTitle(keyword.trim());
    }

    public List<Course> getCoursesByCategory(String category) {
        return courseRepository.findPublishedCoursesByCategory(category);
    }

    public List<Course> getCoursesByLevel(String level) {
        return courseRepository.findByLevel(level);
    }

    public List<String> getAvailableCategories() {
        return courseRepository.findDistinctCategories();
    }

    public List<Course> getCoursesSortedByNewest() {
        return courseRepository.findPublishedCoursesOrderByCreatedAtDesc();
    }

    public List<Course> getCoursesSortedByRating() {
        return courseRepository.findPublishedCoursesOrderByRatingDesc();
    }

    public List<Course> getCoursesSortedByPopularity() {
        return courseRepository.findPublishedCoursesOrderByEnrolledStudentsDesc();
    }

    public String uploadCourseThumbnail(String courseId, MultipartFile file, String instructorId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        // Verify the instructor owns this course
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only update your own courses");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("FILE_EMPTY:File is empty");
        }

        // Check file type (basic validation)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("INVALID_FILE_TYPE:Only images are allowed");
        }

        // Check file size (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("FILE_TOO_LARGE:File size exceeds 10MB limit");
        }

        // TODO: Implement actual file upload to cloud storage (AWS S3, etc.)
        // For now, return a mock URL
        String fileName = courseId + "_thumbnail_" + System.currentTimeMillis() + ".jpg";
        String thumbnailUrl = "https://cdn.example.com/courses/thumbnails/" + fileName;

        course.setThumbnailUrl(thumbnailUrl);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        return thumbnailUrl;
    }

    public void updateCourseStats(String courseId, double newRating, int additionalEnrollments) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return;
        }

        // Update enrollment count
        if (additionalEnrollments != 0) {
            course.setEnrolledStudents(Math.max(0, course.getEnrolledStudents() + additionalEnrollments));
        }

        // Update rating (simplified calculation)
        if (newRating >= 0 && newRating <= 5) {
            // This is a simplified rating calculation
            // In a real application, you'd track individual ratings
            double currentTotalRating = course.getRating() * course.getTotalReviews();
            course.setTotalReviews(course.getTotalReviews() + 1);
            course.setRating((currentTotalRating + newRating) / course.getTotalReviews());
        }

        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    public Map<String, Object> getCourseStatistics(String instructorId) {
        List<Course> instructorCourses = courseRepository.findByInstructorId(instructorId);
        List<Course> publishedCourses = courseRepository.findPublishedCoursesByInstructorId(instructorId);

        int totalCourses = instructorCourses.size();
        int publishedCoursesCount = publishedCourses.size();
        int draftCoursesCount = totalCourses - publishedCoursesCount;

        int totalEnrollments = publishedCourses.stream()
                .mapToInt(course -> course.getEnrolledStudents() != null ? course.getEnrolledStudents() : 0)
                .sum();

        double averageRating = publishedCourses.stream()
                .filter(course -> course.getRating() != null && course.getRating() > 0)
                .mapToDouble(Course::getRating)
                .average()
                .orElse(0.0);

        return Map.of(
            "totalCourses", totalCourses,
            "publishedCourses", publishedCoursesCount,
            "draftCourses", draftCoursesCount,
            "totalEnrollments", totalEnrollments,
            "averageRating", Math.round(averageRating * 100.0) / 100.0
        );
    }
}
