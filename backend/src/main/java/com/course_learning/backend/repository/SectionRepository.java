package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Section;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, String> {
    List<Section> findByCourseIdOrderByOrderIndexAsc(String courseId);

    @Query("SELECT s FROM Section s WHERE s.courseId = :courseId ORDER BY s.orderIndex ASC")
    List<Section> findSectionsByCourseIdOrdered(@Param("courseId") String courseId);

    @Query("SELECT COUNT(s) FROM Section s WHERE s.courseId = :courseId")
    Long countByCourseId(@Param("courseId") String courseId);

    @Query("SELECT MAX(s.orderIndex) FROM Section s WHERE s.courseId = :courseId")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") String courseId);
}
