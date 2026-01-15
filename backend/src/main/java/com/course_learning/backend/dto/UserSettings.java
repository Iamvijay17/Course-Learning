package com.course_learning.backend.dto;

public class UserSettings {
    private NotificationSettings notifications;
    private PrivacySettings privacy;
    private LearningSettings learning;

    // Constructors
    public UserSettings() {
        this.notifications = new NotificationSettings();
        this.privacy = new PrivacySettings();
        this.learning = new LearningSettings();
    }

    // Getters and Setters
    public NotificationSettings getNotifications() { return notifications; }
    public void setNotifications(NotificationSettings notifications) { this.notifications = notifications; }

    public PrivacySettings getPrivacy() { return privacy; }
    public void setPrivacy(PrivacySettings privacy) { this.privacy = privacy; }

    public LearningSettings getLearning() { return learning; }
    public void setLearning(LearningSettings learning) { this.learning = learning; }

    // Nested classes
    public static class NotificationSettings {
        private boolean emailNotifications = true;
        private boolean pushNotifications = false;
        private boolean courseUpdates = true;
        private boolean marketingEmails = false;

        public NotificationSettings() {}

        // Getters and Setters
        public boolean isEmailNotifications() { return emailNotifications; }
        public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

        public boolean isPushNotifications() { return pushNotifications; }
        public void setPushNotifications(boolean pushNotifications) { this.pushNotifications = pushNotifications; }

        public boolean isCourseUpdates() { return courseUpdates; }
        public void setCourseUpdates(boolean courseUpdates) { this.courseUpdates = courseUpdates; }

        public boolean isMarketingEmails() { return marketingEmails; }
        public void setMarketingEmails(boolean marketingEmails) { this.marketingEmails = marketingEmails; }
    }

    public static class PrivacySettings {
        private String profileVisibility = "PUBLIC";
        private boolean showProgress = true;
        private boolean showAchievements = true;

        public PrivacySettings() {}

        // Getters and Setters
        public String getProfileVisibility() { return profileVisibility; }
        public void setProfileVisibility(String profileVisibility) { this.profileVisibility = profileVisibility; }

        public boolean isShowProgress() { return showProgress; }
        public void setShowProgress(boolean showProgress) { this.showProgress = showProgress; }

        public boolean isShowAchievements() { return showAchievements; }
        public void setShowAchievements(boolean showAchievements) { this.showAchievements = showAchievements; }
    }

    public static class LearningSettings {
        private String preferredLanguage = "en";
        private boolean autoplayVideos = true;
        private String transcriptLanguage = "en";

        public LearningSettings() {}

        // Getters and Setters
        public String getPreferredLanguage() { return preferredLanguage; }
        public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

        public boolean isAutoplayVideos() { return autoplayVideos; }
        public void setAutoplayVideos(boolean autoplayVideos) { this.autoplayVideos = autoplayVideos; }

        public String getTranscriptLanguage() { return transcriptLanguage; }
        public void setTranscriptLanguage(String transcriptLanguage) { this.transcriptLanguage = transcriptLanguage; }
    }
}
