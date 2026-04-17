package com.seichou.logos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * EmotionReframingResponse (情绪重构响应 DTO)
 * Data Transfer Object (数据传输对象)，用于封装后端返回给前端的 API 响应数据。
 * 这样做可以隐藏内部的数据库实体结构，只暴露前端需要的数据。
 */
@Data // Lombok 注解，自动生成 getter/setter 等
@Builder // Lombok 注解，提供建造者模式
@Schema(description = "AI 情绪重构分析结果响应对象") // Swagger 注解，用于生成 API 文档说明
public class EmotionReframingResponse {

    @Schema(description = "分析记录的唯一标识 ID")
    private UUID analysisId;

    @Schema(description = "识别出的核心负面情绪", example = "焦虑")
    private String primaryEmotion;

    @Schema(description = "认知重构后的洞察与解释", example = "焦虑是你对未来不确定性的积极预警，说明你非常在乎项目的成功。")
    private String reframedInsight;

    @Schema(description = "量化后的成长资产键值对", example = "{\"自我觉察力\": 10, \"压力代谢值\": 5}")
    private Map<String, Object> growthAssets;

    @Schema(description = "AI 提供的详细洞察（包括真实需求和行动建议）")
    private Map<String, Object> aiInsightDetails;
}
