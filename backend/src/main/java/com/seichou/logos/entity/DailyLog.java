package com.seichou.logos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DailyLog (日常记录实体类)
 * 映射数据库中的 daily_logs 表，用于存储用户输入的原始日志内容。
 */
@Entity // 标记这是一个 JPA 实体类，将被映射到数据库表
@Table(name = "daily_logs") // 指定映射的数据库表名为 daily_logs
@Data // Lombok 注解，自动生成 getter, setter, toString, equals, hashCode 方法
@Builder // Lombok 注解，提供建造者模式 (Builder Pattern) 来方便地创建对象实例
@NoArgsConstructor // Lombok 注解，生成无参构造函数 (JPA 规范要求)
@AllArgsConstructor // Lombok 注解，生成包含所有属性的构造函数
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
}
