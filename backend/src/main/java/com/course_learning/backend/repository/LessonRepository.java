package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Lesson;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, String> {
    List<Lesson> findByModuleIdOrderByOrderIndexAsc(String moduleId);

    @Query("SELECT l FROM Lesson l WHERE l.moduleId = :moduleId ORDER BY l.orderIndex ASC")
    List<Lesson> findLessonsByModuleIdOrdered(@Param("moduleId") String moduleId);

    @Query("SELECT l FROM Lesson l WHERE l.moduleId IN :moduleIds ORDER BY l.orderIndex ASC")
    List<Lesson> findLessonsByModuleIds(@Param("moduleIds") List<String> moduleIds);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.moduleId = :moduleId")
    Long countByModuleId(@Param("moduleId") String moduleId);

    @Query("SELECT MAX(l.orderIndex) FROM Lesson l WHERE l.moduleId = :moduleId")
    Integer findMaxOrderIndexByModuleId(@Param("moduleId") String moduleId);

    @Query("SELECT l FROM Lesson l WHERE l.moduleId IN (SELECT m.moduleId FROM Module m WHERE m.sectionId IN (SELECT s.sectionId FROM Section s WHERE s.courseId = :courseId)) ORDER BY l.orderIndex ASC")
    List<Lesson> findLessonsByCourseId(@Param("courseId") String courseId);

    @Query("SELECT l FROM Lesson l WHERE l.isPreview = true AND l.moduleId IN (SELECT m.moduleId FROM Module m WHERE m.sectionId IN (SELECT s.sectionId FROM Section s WHERE s.courseId = :courseId)) ORDER BY l.orderIndex ASC")
    List<Lesson> findPreviewLessonsByCourseId(@Param("courseId") String courseId);
}
