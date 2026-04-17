package com.seichou.logos.repository;

import com.seichou.logos.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, UUID> {
    
    // 根据用户 ID 查找所有日志，按创建时间降序排列
    List<DailyLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 复杂 JPA 查询示例：
     * 查找某个用户在指定时间范围内的所有日志，并关联查询其情绪分析结果。
     * 使用 LEFT JOIN FETCH 解决 N+1 查询问题。
     */
    @Query("SELECT d FROM DailyLog d LEFT JOIN FETCH d.emotionAnalysis WHERE d.userId = :userId AND d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
    List<DailyLog> findLogsWithAnalysisByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
