package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Module;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, String> {
    List<Module> findBySectionIdOrderByOrderIndexAsc(String sectionId);

    @Query("SELECT m FROM Module m WHERE m.sectionId = :sectionId ORDER BY m.orderIndex ASC")
    List<Module> findModulesBySectionIdOrdered(@Param("sectionId") String sectionId);

    @Query("SELECT COUNT(m) FROM Module m WHERE m.sectionId = :sectionId")
    Long countBySectionId(@Param("sectionId") String sectionId);

    @Query("SELECT MAX(m.orderIndex) FROM Module m WHERE m.sectionId = :sectionId")
    Integer findMaxOrderIndexBySectionId(@Param("sectionId") String sectionId);

    @Query("SELECT m FROM Module m WHERE m.sectionId IN :sectionIds ORDER BY m.orderIndex ASC")
    List<Module> findModulesBySectionIds(@Param("sectionIds") List<String> sectionIds);

    @Query("SELECT m FROM Module m WHERE m.sectionId IN (SELECT s.sectionId FROM Section s WHERE s.courseId = :courseId) ORDER BY m.orderIndex ASC")
    List<Module> findModulesByCourseId(@Param("courseId") String courseId);
}
