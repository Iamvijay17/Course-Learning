package com.course_learning.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.course_learning.backend.model.Course;
import com.course_learning.backend.model.Section;
import com.course_learning.backend.repository.CourseRepository;
import com.course_learning.backend.repository.SectionRepository;

@Service
public class SectionService {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    public List<Section> getSectionsByCourse(String courseId, String instructorId) {
        // Verify course ownership
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only access your own courses");
        }

        return sectionRepository.findSectionsByCourseIdOrdered(courseId);
    }

    public Section createSection(String courseId, Section sectionData, String instructorId) {
        // Verify course ownership
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Set order index if not provided
        if (sectionData.getOrderIndex() == null) {
            Integer maxOrder = sectionRepository.findMaxOrderIndexByCourseId(courseId);
            sectionData.setOrderIndex(maxOrder != null ? maxOrder + 1 : 0);
        }

        Section section = new Section();
        section.setCourseId(courseId);
        section.setTitle(sectionData.getTitle());
        section.setDescription(sectionData.getDescription());
        section.setOrderIndex(sectionData.getOrderIndex());
        section.setEstimatedHours(sectionData.getEstimatedHours());
        section.setCreatedAt(LocalDateTime.now());
        section.setUpdatedAt(LocalDateTime.now());

        return sectionRepository.save(section);
    }

    public Section updateSection(String sectionId, Section sectionData, String instructorId) {
        Section existingSection = sectionRepository.findById(sectionId).orElse(null);
        if (existingSection == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        // Verify course ownership
        Course course = courseRepository.findById(existingSection.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Update fields
        if (sectionData.getTitle() != null) {
            existingSection.setTitle(sectionData.getTitle());
        }
        if (sectionData.getDescription() != null) {
            existingSection.setDescription(sectionData.getDescription());
        }
        if (sectionData.getOrderIndex() != null) {
            existingSection.setOrderIndex(sectionData.getOrderIndex());
        }
        if (sectionData.getEstimatedHours() != null) {
            existingSection.setEstimatedHours(sectionData.getEstimatedHours());
        }

        existingSection.setUpdatedAt(LocalDateTime.now());
        return sectionRepository.save(existingSection);
    }

    public boolean deleteSection(String sectionId, String instructorId) {
        Section section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            return false;
        }

        // Verify course ownership
        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        sectionRepository.deleteById(sectionId);
        return true;
    }

    public void reorderSections(String courseId, List<String> sectionIds, String instructorId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        for (int i = 0; i < sectionIds.size(); i++) {
            Section section = sectionRepository.findById(sectionIds.get(i)).orElse(null);
            if (section != null && section.getCourseId().equals(courseId)) {
                section.setOrderIndex(i);
                section.setUpdatedAt(LocalDateTime.now());
                sectionRepository.save(section);
            }
        }
    }
}
