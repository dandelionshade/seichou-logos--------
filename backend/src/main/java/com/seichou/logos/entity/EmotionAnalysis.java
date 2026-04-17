package com.seichou.logos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data // Lombok 注解，自动生成 getter/setter 等
@Builder // Lombok 注解，提供建造者模式
@NoArgsConstructor // Lombok 注解，生成无参构造函数
@AllArgsConstructor // Lombok 注解，生成全参构造函数
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
}
