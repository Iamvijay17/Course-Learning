package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.course_learning.backend.model.Video;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, String> {
    Optional<Video> findByLessonId(String lessonId);

    List<Video> findByStatus(String status);

    List<Video> findByStatusIn(List<String> statuses);

    @Query("SELECT v FROM Video v WHERE v.lessonId IN :lessonIds")
    List<Video> findVideosByLessonIds(@Param("lessonIds") List<String> lessonIds);

    @Query("SELECT v FROM Video v WHERE v.uploadId = :uploadId")
    Optional<Video> findByUploadId(@Param("uploadId") String uploadId);

    @Query("SELECT COUNT(v) FROM Video v WHERE v.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT v FROM Video v WHERE v.status IN ('PROCESSING', 'UPLOADING')")
    List<Video> findProcessingVideos();

    @Query("SELECT v FROM Video v WHERE v.createdAt < :cutoffDate AND v.status != 'READY'")
    List<Video> findStaleVideos(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
