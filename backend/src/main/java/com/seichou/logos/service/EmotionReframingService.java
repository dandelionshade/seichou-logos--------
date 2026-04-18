package com.seichou.logos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seichou.logos.dto.EmotionReframingResponse;
import com.seichou.logos.entity.DailyLog;
import com.seichou.logos.entity.EmotionAnalysis;
import com.seichou.logos.repository.DailyLogRepository;
import com.seichou.logos.repository.EmotionAnalysisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EmotionReframingService (情绪重构服务)
 * 核心业务逻辑层：负责调用 DeepSeek API，解析结果，并保存到数据库。
 */
@Service
public class EmotionReframingService {

    private static final Logger logger = Logger.getLogger(EmotionReframingService.class.getName());

    private final DailyLogRepository dailyLogRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.key}")
    private String apiKey;

    public EmotionReframingService(DailyLogRepository dailyLogRepository, EmotionAnalysisRepository emotionAnalysisRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.dailyLogRepository = dailyLogRepository;
        this.emotionAnalysisRepository = emotionAnalysisRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 核心方法：执行情绪重构
     * @param dailyLog 用户提交的日志实体
     * @param therapyMode 心理学流派 (adlerian, cbt, mindfulness)
     * @return EmotionReframingResponse 返回给前端的 DTO
     */
    @Transactional
    public EmotionReframingResponse reframeEmotion(DailyLog dailyLog, String therapyMode) {
        logger.info("开始处理情绪重构请求，日志ID: " + dailyLog.getLogId() + ", 流派: " + therapyMode);

        // 1. 构造发给 DeepSeek 的 Prompt
        String prompt = buildPrompt(dailyLog.getContent(), therapyMode);

        // 2. 准备 HTTP 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 3. 准备请求体 (遵循 DeepSeek Chat API 规范)
        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful psychological assistant. Always output valid JSON without markdown formatting."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. 发送 POST 请求调用 DeepSeek API
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            // 5. 解析 API 返回的 JSON 结果
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String aiContent = rootNode.path("choices").get(0).path("message").path("content").asText();
            
            // 将 AI 返回的 JSON 字符串反序列化为 DTO 对象
            EmotionReframingResponse reframingResponse = objectMapper.readValue(aiContent, EmotionReframingResponse.class);

            // 6. 将结果持久化到数据库
            saveAnalysisResult(dailyLog, reframingResponse);

            return reframingResponse;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "调用 DeepSeek API 失败", e);
            throw new RuntimeException("AI 情绪重构失败，请稍后重试", e);
        }
    }

    /**
     * 将 AI 分析结果保存到数据库
     */
    private void saveAnalysisResult(DailyLog dailyLog, EmotionReframingResponse response) {
        // 1. 确保 DailyLog 已保存 (如果还没保存的话)
        if (dailyLog.getLogId() == null) {
            dailyLogRepository.save(dailyLog);
        }

        // 2. 创建 EmotionAnalysis 实体
        EmotionAnalysis analysis = new EmotionAnalysis();
        analysis.setDailyLog(dailyLog);
        analysis.setPrimaryEmotion(response.getPrimaryEmotion());
        analysis.setReframedInsight(response.getReframedInsight());
        analysis.setGrowthAssets(response.getGrowthAssets());
        analysis.setAiInsightDetails(response.getAiInsightDetails());

        // 3. 保存分析结果
        emotionAnalysisRepository.save(analysis);
        logger.info("已成功保存情绪分析结果，日志ID: " + dailyLog.getLogId());
    }

    /**
     * 辅助方法：构建 Prompt (提示词工程)
     */
    private String buildPrompt(String content, String therapyMode) {
        Map<String, String> modeDescriptions = Map.of(
            "adlerian", "阿德勒心理学（强调目的论、课题分离、把挫折视为成长的阶梯）",
            "cbt", "认知行为疗法 CBT（强调识别认知扭曲、打破负面思维循环、客观验证）",
            "mindfulness", "正念与接纳承诺疗法 ACT（强调不评判地接纳负面情绪、认知解离、关注当下）"
        );
        String selectedMode = modeDescriptions.getOrDefault(therapyMode, modeDescriptions.get("adlerian"));

            return "你是一名资深心理咨询师，精通【" + selectedMode + "】。\n"
                + "用户刚刚经历了一些负面情绪，记录如下：\n"
                + "\"" + content + "\"\n\n"
                + "请严格按照以下 JSON 格式输出你的分析（不要包含任何 Markdown 标记如 ```json，直接输出纯 JSON 对象）：\n"
                + "{\n"
                + "  \"primary_emotion\": \"核心情绪（如：焦虑、内疚、愤怒）\",\n"
                + "  \"reframed_insight\": \"基于该流派的深度洞察。用温暖、接纳的语气，向用户解释这种情绪背后的积极意义或保护机制（50-80字）。\",\n"
                + "  \"growth_assets\": {\n"
                + "    \"资产名称1 (如: 风险感知力)\": 增加的数值(1-20),\n"
                + "    \"资产名称2 (如: 责任心)\": 增加的数值(1-20)\n"
                + "  },\n"
                + "  \"ai_insight_details\": {\n"
                + "    \"real_need\": \"用户潜意识里的真实需求是什么？\",\n"
                + "    \"recommendation\": \"给出一个微小、具体、今天就能完成的行动建议（30字以内）。\"\n"
                + "  }\n"
                + "}\n"
                + "支持中日双语输出（根据用户输入的语言自动适配）。";
    }
}
