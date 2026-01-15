package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Module;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, String> {
    List<Module> findByCourseIdOrderByOrderIndexAsc(String courseId);

    @Query("SELECT m FROM Module m WHERE m.courseId = :courseId ORDER BY m.orderIndex ASC")
    List<Module> findModulesByCourseIdOrdered(@Param("courseId") String courseId);

    @Query("SELECT COUNT(m) FROM Module m WHERE m.courseId = :courseId")
    Long countByCourseId(@Param("courseId") String courseId);

    @Query("SELECT MAX(m.orderIndex) FROM Module m WHERE m.courseId = :courseId")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") String courseId);
}
