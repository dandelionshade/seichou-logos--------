package com.seichou.logos.repository;

import com.seichou.logos.entity.EmotionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * EmotionAnalysisRepository (情绪分析结果数据访问层)
 * 继承 JpaRepository，提供对 emotion_analysis 表的基础 CRUD 操作。
 * 泛型参数 <EmotionAnalysis, UUID> 分别代表：操作的实体类类型，以及该实体类主键的类型。
 */
@Repository // 标记这是一个数据访问层组件
public interface EmotionAnalysisRepository extends JpaRepository<EmotionAnalysis, UUID> {
    // 可以在这里添加自定义查询方法，例如根据 logId 查询对应的分析结果
    // EmotionAnalysis findByLogId(UUID logId);
}
