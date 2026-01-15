package com.course_learning.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.course_learning.backend.model.Course;
import com.course_learning.backend.model.Section;
import com.course_learning.backend.model.Module;
import com.course_learning.backend.model.Lesson;
import com.course_learning.backend.model.Video;
import com.course_learning.backend.repository.CourseRepository;
import com.course_learning.backend.repository.SectionRepository;
import com.course_learning.backend.repository.ModuleRepository;
import com.course_learning.backend.repository.LessonRepository;
import com.course_learning.backend.repository.VideoRepository;

@Service
public class ContentService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private VideoRepository videoRepository;

    // ========== MODULE OPERATIONS ==========

    public List<Module> getModulesBySection(String sectionId, String instructorId) {
        // Verify course ownership through section
        Section section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only access your own courses");
        }

        return moduleRepository.findModulesBySectionIdOrdered(sectionId);
    }

    public Module createModule(String sectionId, Module moduleData, String instructorId) {
        // Verify course ownership through section
        Section section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Set order index if not provided
        if (moduleData.getOrderIndex() == null) {
            Integer maxOrder = moduleRepository.findMaxOrderIndexBySectionId(sectionId);
            moduleData.setOrderIndex(maxOrder != null ? maxOrder + 1 : 0);
        }

        Module module = new Module();
        module.setSectionId(sectionId);
        module.setTitle(moduleData.getTitle());
        module.setDescription(moduleData.getDescription());
        module.setOrderIndex(moduleData.getOrderIndex());
        module.setCreatedAt(LocalDateTime.now());
        module.setUpdatedAt(LocalDateTime.now());

        return moduleRepository.save(module);
    }

    public Module updateModule(String moduleId, Module moduleData, String instructorId) {
        Module existingModule = moduleRepository.findById(moduleId).orElse(null);
        if (existingModule == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        // Verify course ownership through section
        Section section = sectionRepository.findById(existingModule.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Update fields
        if (moduleData.getTitle() != null) {
            existingModule.setTitle(moduleData.getTitle());
        }
        if (moduleData.getDescription() != null) {
            existingModule.setDescription(moduleData.getDescription());
        }
        if (moduleData.getOrderIndex() != null) {
            existingModule.setOrderIndex(moduleData.getOrderIndex());
        }

        existingModule.setUpdatedAt(LocalDateTime.now());
        return moduleRepository.save(existingModule);
    }

    public boolean deleteModule(String moduleId, String instructorId) {
        Module module = moduleRepository.findById(moduleId).orElse(null);
        if (module == null) {
            return false;
        }

        // Verify course ownership through section
        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Delete associated lessons and videos
        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        for (Lesson lesson : lessons) {
            // Delete associated video if exists
            videoRepository.findByLessonId(lesson.getLessonId()).ifPresent(video -> {
                videoRepository.delete(video);
                // TODO: Delete actual video file from storage
            });
            lessonRepository.delete(lesson);
        }

        moduleRepository.deleteById(moduleId);
        return true;
    }

    // ========== LESSON OPERATIONS ==========

    public List<Lesson> getLessonsByModule(String moduleId, String instructorId) {
        Module module = moduleRepository.findById(moduleId).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        // Verify course ownership through section
        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only access your own courses");
        }

        return lessonRepository.findLessonsByModuleIdOrdered(moduleId);
    }

    public Lesson createLesson(String moduleId, Lesson lessonData, String instructorId) {
        Module module = moduleRepository.findById(moduleId).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        // Verify course ownership through section
        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only access your own courses");
        }

        // Set order index if not provided
        if (lessonData.getOrderIndex() == null) {
            Integer maxOrder = lessonRepository.findMaxOrderIndexByModuleId(moduleId);
            lessonData.setOrderIndex(maxOrder != null ? maxOrder + 1 : 0);
        }

        Lesson lesson = new Lesson();
        lesson.setModuleId(moduleId);
        lesson.setTitle(lessonData.getTitle());
        lesson.setDescription(lessonData.getDescription());
        lesson.setOrderIndex(lessonData.getOrderIndex());
        lesson.setIsPreview(lessonData.getIsPreview() != null ? lessonData.getIsPreview() : false);
        lesson.setCreatedAt(LocalDateTime.now());
        lesson.setUpdatedAt(LocalDateTime.now());

        return lessonRepository.save(lesson);
    }

    public Lesson updateLesson(String lessonId, Lesson lessonData, String instructorId) {
        Lesson existingLesson = lessonRepository.findById(lessonId).orElse(null);
        if (existingLesson == null) {
            throw new RuntimeException("LESSON_NOT_FOUND:Lesson not found");
        }

        // Verify course ownership through module
        Module module = moduleRepository.findById(existingLesson.getModuleId()).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Update fields
        if (lessonData.getTitle() != null) {
            existingLesson.setTitle(lessonData.getTitle());
        }
        if (lessonData.getDescription() != null) {
            existingLesson.setDescription(lessonData.getDescription());
        }
        if (lessonData.getOrderIndex() != null) {
            existingLesson.setOrderIndex(lessonData.getOrderIndex());
        }
        if (lessonData.getIsPreview() != null) {
            existingLesson.setIsPreview(lessonData.getIsPreview());
        }

        existingLesson.setUpdatedAt(LocalDateTime.now());
        return lessonRepository.save(existingLesson);
    }

    public boolean deleteLesson(String lessonId, String instructorId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return false;
        }

        // Verify course ownership through module
        Module module = moduleRepository.findById(lesson.getModuleId()).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Delete associated video if exists
        videoRepository.findByLessonId(lessonId).ifPresent(video -> {
            videoRepository.delete(video);
            // TODO: Delete actual video file from storage
        });

        lessonRepository.deleteById(lessonId);
        return true;
    }

    // ========== VIDEO OPERATIONS ==========

    public Video uploadVideo(String lessonId, MultipartFile file, String instructorId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            throw new RuntimeException("LESSON_NOT_FOUND:Lesson not found");
        }

        // Verify course ownership through module
        Module module = moduleRepository.findById(lesson.getModuleId()).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("FILE_EMPTY:File is empty");
        }

        // Check file type (video validation)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new RuntimeException("INVALID_FILE_TYPE:Only video files are allowed");
        }

        // Check file size (500MB limit for videos)
        if (file.getSize() > 500 * 1024 * 1024) {
            throw new RuntimeException("FILE_TOO_LARGE:File size exceeds 500MB limit");
        }

        // Check if lesson already has a video
        Video existingVideo = videoRepository.findByLessonId(lessonId).orElse(null);
        if (existingVideo != null) {
            throw new RuntimeException("VIDEO_EXISTS:Lesson already has a video. Delete existing video first.");
        }

        // Generate unique filename
        String fileName = lessonId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String uploadId = java.util.UUID.randomUUID().toString();

        Video video = new Video();
        video.setLessonId(lessonId);
        video.setOriginalFileName(file.getOriginalFilename());
        video.setFileName(fileName);
        video.setFileSize(file.getSize());
        video.setMimeType(contentType);
        video.setStatus("UPLOADING");
        video.setProcessingProgress(0);
        video.setUploadId(uploadId);
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());

        Video savedVideo = videoRepository.save(video);

        // TODO: Start actual video upload to cloud storage (AWS S3, etc.)
        // For now, simulate upload completion
        completeVideoUpload(savedVideo.getVideoId());

        return savedVideo;
    }

    public Video getVideoByLesson(String lessonId) {
        return videoRepository.findByLessonId(lessonId).orElse(null);
    }

    public boolean deleteVideo(String lessonId, String instructorId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return false;
        }

        // Verify course ownership through module
        Module module = moduleRepository.findById(lesson.getModuleId()).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        Video video = videoRepository.findByLessonId(lessonId).orElse(null);
        if (video == null) {
            return false;
        }

        videoRepository.delete(video);
        // TODO: Delete actual video file from storage

        return true;
    }

    private void completeVideoUpload(String videoId) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return;

        // Simulate video processing completion
        video.setStatus("READY");
        video.setProcessingProgress(100);
        video.setFileUrl("https://cdn.example.com/videos/" + video.getFileName());
        video.setThumbnailUrl("https://cdn.example.com/thumbnails/" + videoId + ".jpg");
        video.setDuration("10:30"); // Example duration
        video.setResolution("1920x1080");
        video.setUpdatedAt(LocalDateTime.now());

        videoRepository.save(video);
    }

    // ========== COURSE CONTENT STRUCTURE ==========

    public Map<String, Object> getCourseContentStructure(String courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        List<Section> sections = sectionRepository.findSectionsByCourseIdOrdered(courseId);
        List<String> sectionIds = sections.stream().map(Section::getSectionId).collect(Collectors.toList());
        List<Module> modules = moduleRepository.findModulesBySectionIds(sectionIds);
        List<String> moduleIds = modules.stream().map(Module::getModuleId).collect(Collectors.toList());

        List<Lesson> allLessons = lessonRepository.findLessonsByModuleIds(moduleIds);
        List<String> lessonIds = allLessons.stream().map(Lesson::getLessonId).collect(Collectors.toList());

        List<Video> videos = videoRepository.findVideosByLessonIds(lessonIds);

        // Build the structure
        List<Map<String, Object>> moduleStructure = modules.stream().map(module -> {
            List<Map<String, Object>> lessonStructure = allLessons.stream()
                .filter(lesson -> lesson.getModuleId().equals(module.getModuleId()))
                .map(lesson -> {
                    Video video = videos.stream()
                        .filter(v -> v.getLessonId().equals(lesson.getLessonId()))
                        .findFirst().orElse(null);

                    return Map.of(
                        "lessonId", lesson.getLessonId(),
                        "title", lesson.getTitle(),
                        "description", lesson.getDescription(),
                        "orderIndex", lesson.getOrderIndex(),
                        "isPreview", lesson.getIsPreview(),
                        "video", video != null ? Map.of(
                            "videoId", video.getVideoId(),
                            "duration", video.getDuration(),
                            "status", video.getStatus(),
                            "fileUrl", video.getFileUrl(),
                            "thumbnailUrl", video.getThumbnailUrl()
                        ) : null
                    );
                })
                .collect(Collectors.toList());

            return Map.of(
                "moduleId", module.getModuleId(),
                "title", module.getTitle(),
                "description", module.getDescription(),
                "orderIndex", module.getOrderIndex(),
                "lessons", lessonStructure
            );
        }).collect(Collectors.toList());

        return Map.of(
            "courseId", courseId,
            "courseTitle", course.getTitle(),
            "modules", moduleStructure
        );
    }

    public List<Lesson> getCoursePreviewLessons(String courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }

        return lessonRepository.findPreviewLessonsByCourseId(courseId);
    }

    // ========== UTILITY METHODS ==========

    public void reorderModules(String courseId, List<String> moduleIds, String instructorId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            throw new RuntimeException("COURSE_NOT_FOUND:Course not found");
        }
        if (!course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        for (int i = 0; i < moduleIds.size(); i++) {
            Module module = moduleRepository.findById(moduleIds.get(i)).orElse(null);
            if (module != null) {
                // Verify the module belongs to the course through sections
                Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
                if (section != null && section.getCourseId().equals(courseId)) {
                    module.setOrderIndex(i);
                    module.setUpdatedAt(LocalDateTime.now());
                    moduleRepository.save(module);
                }
            }
        }
    }

    public void reorderLessons(String moduleId, List<String> lessonIds, String instructorId) {
        Module module = moduleRepository.findById(moduleId).orElse(null);
        if (module == null) {
            throw new RuntimeException("MODULE_NOT_FOUND:Module not found");
        }

        Section section = sectionRepository.findById(module.getSectionId()).orElse(null);
        if (section == null) {
            throw new RuntimeException("SECTION_NOT_FOUND:Section not found");
        }

        Course course = courseRepository.findById(section.getCourseId()).orElse(null);
        if (course == null || !course.getInstructorId().equals(instructorId)) {
            throw new RuntimeException("UNAUTHORIZED:You can only modify your own courses");
        }

        for (int i = 0; i < lessonIds.size(); i++) {
            Lesson lesson = lessonRepository.findById(lessonIds.get(i)).orElse(null);
            if (lesson != null && lesson.getModuleId().equals(moduleId)) {
                lesson.setOrderIndex(i);
                lesson.setUpdatedAt(LocalDateTime.now());
                lessonRepository.save(lesson);
            }
        }
    }
}
