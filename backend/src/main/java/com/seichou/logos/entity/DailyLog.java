package com.seichou.logos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DailyLog (日常记录实体类)
 * 映射数据库中的 daily_logs 表，用于存储用户输入的原始日志内容。
 */
@Entity // 标记这是一个 JPA 实体类，将被映射到数据库表
@Table(name = "daily_logs") // 指定映射的数据库表名为 daily_logs
public class DailyLog {

    @Id // 标记此属性为主键
    @GeneratedValue(strategy = GenerationType.UUID) // 指定主键生成策略为 UUID
    @Column(name = "log_id", updatable = false, nullable = false) // 映射到列 log_id，不可更新，不能为空
    private UUID logId;

    @Column(name = "user_id", nullable = false) // 映射到列 user_id，表示该日志属于哪个用户
    private UUID userId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false) // 映射到列 content，类型为 TEXT，存储用户输入的具体文字
    private String content;

    @Column(name = "mood_score")
    private Integer moodScore;

    @Column(name = "log_date", nullable = false)
    private java.time.LocalDate logDate;

    @CreationTimestamp // Hibernate 注解，在实体首次保存到数据库时自动填充当前时间
    @Column(name = "created_at", updatable = false) // 映射到列 created_at，不可更新
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "dailyLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EmotionAnalysis emotionAnalysis;

    public DailyLog() {
    }

    public DailyLog(UUID logId, UUID userId, String content, Integer moodScore, java.time.LocalDate logDate, LocalDateTime createdAt, EmotionAnalysis emotionAnalysis) {
        this.logId = logId;
        this.userId = userId;
        this.content = content;
        this.moodScore = moodScore;
        this.logDate = logDate;
        this.createdAt = createdAt;
        this.emotionAnalysis = emotionAnalysis;
    }

    public static DailyLogBuilder builder() { return new DailyLogBuilder(); }

    public UUID getLogId() { return logId; }
    public void setLogId(UUID logId) { this.logId = logId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getMoodScore() { return moodScore; }
    public void setMoodScore(Integer moodScore) { this.moodScore = moodScore; }
    public java.time.LocalDate getLogDate() { return logDate; }
    public void setLogDate(java.time.LocalDate logDate) { this.logDate = logDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public EmotionAnalysis getEmotionAnalysis() { return emotionAnalysis; }
    public void setEmotionAnalysis(EmotionAnalysis emotionAnalysis) { this.emotionAnalysis = emotionAnalysis; }

    public static class DailyLogBuilder {
        private UUID logId;
        private UUID userId;
        private String content;
        private Integer moodScore;
        private java.time.LocalDate logDate;
        private LocalDateTime createdAt;
        private EmotionAnalysis emotionAnalysis;

        public DailyLogBuilder logId(UUID logId) { this.logId = logId; return this; }
        public DailyLogBuilder userId(UUID userId) { this.userId = userId; return this; }
        public DailyLogBuilder content(String content) { this.content = content; return this; }
        public DailyLogBuilder moodScore(Integer moodScore) { this.moodScore = moodScore; return this; }
        public DailyLogBuilder logDate(java.time.LocalDate logDate) { this.logDate = logDate; return this; }
        public DailyLogBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DailyLogBuilder emotionAnalysis(EmotionAnalysis emotionAnalysis) { this.emotionAnalysis = emotionAnalysis; return this; }

        public DailyLog build() { return new DailyLog(logId, userId, content, moodScore, logDate, createdAt, emotionAnalysis); }
    }
}
