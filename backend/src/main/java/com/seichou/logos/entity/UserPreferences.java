package com.seichou.logos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 20)
    private String theme = "dark";

    @Column(length = 10)
    private String language = "en";

    @Column(name = "ai_personality", length = 30)
    private String aiPersonality = "empathetic";

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled = true;

    @Column(name = "data_privacy", length = 20)
    private String dataPrivacy = "standard";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserPreferences() {
    }

    public UserPreferences(UUID userId, User user, String theme, String language, String aiPersonality, Boolean notificationsEnabled, String dataPrivacy, LocalDateTime updatedAt) {
        this.userId = userId;
        this.user = user;
        this.theme = theme;
        this.language = language;
        this.aiPersonality = aiPersonality;
        this.notificationsEnabled = notificationsEnabled;
        this.dataPrivacy = dataPrivacy;
        this.updatedAt = updatedAt;
    }

    public static UserPreferencesBuilder builder() {
        return new UserPreferencesBuilder();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getAiPersonality() { return aiPersonality; }
    public void setAiPersonality(String aiPersonality) { this.aiPersonality = aiPersonality; }
    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public String getDataPrivacy() { return dataPrivacy; }
    public void setDataPrivacy(String dataPrivacy) { this.dataPrivacy = dataPrivacy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class UserPreferencesBuilder {
        private UUID userId;
        private User user;
        private String theme = "dark";
        private String language = "en";
        private String aiPersonality = "empathetic";
        private Boolean notificationsEnabled = true;
        private String dataPrivacy = "standard";
        private LocalDateTime updatedAt;

        public UserPreferencesBuilder userId(UUID userId) { this.userId = userId; return this; }
        public UserPreferencesBuilder user(User user) { this.user = user; return this; }
        public UserPreferencesBuilder theme(String theme) { this.theme = theme; return this; }
        public UserPreferencesBuilder language(String language) { this.language = language; return this; }
        public UserPreferencesBuilder aiPersonality(String aiPersonality) { this.aiPersonality = aiPersonality; return this; }
        public UserPreferencesBuilder notificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; return this; }
        public UserPreferencesBuilder dataPrivacy(String dataPrivacy) { this.dataPrivacy = dataPrivacy; return this; }
        public UserPreferencesBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserPreferences build() {
            return new UserPreferences(userId, user, theme, language, aiPersonality, notificationsEnabled, dataPrivacy, updatedAt);
        }
    }
}
