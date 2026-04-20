package com.seichou.logos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * EmotionAnalysis (情绪分析结果实体类)
 * 映射数据库中的 emotion_analysis 表，用于存储 AI 对日志分析后生成的结构化数据。
 */
@Entity // 标记这是一个 JPA 实体类
@Table(name = "emotion_analysis") // 指定映射的数据库表名为 emotion_analysis
public class EmotionAnalysis {

    @Id // 标记为主键
    @GeneratedValue(strategy = GenerationType.UUID) // UUID 生成策略
    @Column(name = "analysis_id", updatable = false, nullable = false)
    private UUID analysisId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private DailyLog dailyLog;

    @Column(name = "primary_emotion", length = 50) // 核心情绪名称，如"焦虑"
    private String primaryEmotion;

    @Column(name = "reframed_insight", columnDefinition = "TEXT") // 认知重构后的洞察解释
    private String reframedInsight;

    /**
     * 存储量化后的成长资产。
     * 使用 PostgreSQL 特有的 JSONB 类型存储，方便后续的灵活查询和扩展。
     * @JdbcTypeCode(SqlTypes.JSON) 告诉 Hibernate 将这个 Map 映射为数据库的 JSON 字段。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "growth_assets", columnDefinition = "jsonb")
    private Map<String, Object> growthAssets;

    /**
     * 存储 AI 提供的详细洞察（如真实需求、行动建议等）。
     * 同样使用 JSONB 格式存储非结构化或半结构化数据。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_insight_details", columnDefinition = "jsonb")
    private Map<String, Object> aiInsightDetails;

    @CreationTimestamp // 自动填充创建时间
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public EmotionAnalysis() {
    }

    public EmotionAnalysis(UUID analysisId, DailyLog dailyLog, String primaryEmotion, String reframedInsight, Map<String, Object> growthAssets, Map<String, Object> aiInsightDetails, LocalDateTime createdAt) {
        this.analysisId = analysisId;
        this.dailyLog = dailyLog;
        this.primaryEmotion = primaryEmotion;
        this.reframedInsight = reframedInsight;
        this.growthAssets = growthAssets;
        this.aiInsightDetails = aiInsightDetails;
        this.createdAt = createdAt;
    }

    public static EmotionAnalysisBuilder builder() { return new EmotionAnalysisBuilder(); }

    public UUID getAnalysisId() { return analysisId; }
    public void setAnalysisId(UUID analysisId) { this.analysisId = analysisId; }
    public DailyLog getDailyLog() { return dailyLog; }
    public void setDailyLog(DailyLog dailyLog) { this.dailyLog = dailyLog; }
    public String getPrimaryEmotion() { return primaryEmotion; }
    public void setPrimaryEmotion(String primaryEmotion) { this.primaryEmotion = primaryEmotion; }
    public String getReframedInsight() { return reframedInsight; }
    public void setReframedInsight(String reframedInsight) { this.reframedInsight = reframedInsight; }
    public Map<String, Object> getGrowthAssets() { return growthAssets; }
    public void setGrowthAssets(Map<String, Object> growthAssets) { this.growthAssets = growthAssets; }
    public Map<String, Object> getAiInsightDetails() { return aiInsightDetails; }
    public void setAiInsightDetails(Map<String, Object> aiInsightDetails) { this.aiInsightDetails = aiInsightDetails; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class EmotionAnalysisBuilder {
        private UUID analysisId;
        private DailyLog dailyLog;
        private String primaryEmotion;
        private String reframedInsight;
        private Map<String, Object> growthAssets;
        private Map<String, Object> aiInsightDetails;
        private LocalDateTime createdAt;

        public EmotionAnalysisBuilder analysisId(UUID analysisId) { this.analysisId = analysisId; return this; }
        public EmotionAnalysisBuilder dailyLog(DailyLog dailyLog) { this.dailyLog = dailyLog; return this; }
        public EmotionAnalysisBuilder primaryEmotion(String primaryEmotion) { this.primaryEmotion = primaryEmotion; return this; }
        public EmotionAnalysisBuilder reframedInsight(String reframedInsight) { this.reframedInsight = reframedInsight; return this; }
        public EmotionAnalysisBuilder growthAssets(Map<String, Object> growthAssets) { this.growthAssets = growthAssets; return this; }
        public EmotionAnalysisBuilder aiInsightDetails(Map<String, Object> aiInsightDetails) { this.aiInsightDetails = aiInsightDetails; return this; }
        public EmotionAnalysisBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public EmotionAnalysis build() { return new EmotionAnalysis(analysisId, dailyLog, primaryEmotion, reframedInsight, growthAssets, aiInsightDetails, createdAt); }
    }
}
