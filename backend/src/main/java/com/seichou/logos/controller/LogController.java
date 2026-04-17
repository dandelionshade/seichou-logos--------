package com.seichou.logos.controller;

import com.seichou.logos.dto.EmotionReframingResponse;
import com.seichou.logos.entity.DailyLog;
import com.seichou.logos.service.EmotionReframingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * LogController (日志控制器)
 * 负责处理前端发来的 HTTP 请求，提供日志记录与情绪重构的 API 接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Daily Log Controller", description = "日常记录与情绪重构 API")
public class LogController {

    private final EmotionReframingService reframingService;
    private final com.seichou.logos.repository.DailyLogRepository dailyLogRepository;

    /**
     * 提交日志并进行 AI 情绪重构的接口
     */
    @PostMapping("/reframe")
    @Operation(summary = "提交日志并进行 AI 情绪重构")
    public ResponseEntity<EmotionReframingResponse> reframeEmotion(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        // 获取前端传递的心理学流派，默认为阿德勒心理学
        String therapyMode = request.getOrDefault("therapyMode", "adlerian");
        
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 1. 创建并保存 DailyLog 实体对象
        DailyLog log = DailyLog.builder()
                .userId(user.getUserId())
                .content(content)
                .logDate(java.time.LocalDate.now())
                .build();
        
        DailyLog savedLog = dailyLogRepository.save(log);

        // 2. 调用 Service 层执行 AI 情绪重构逻辑，传入流派参数
        // Service 内部会负责保存 EmotionAnalysis
        EmotionReframingResponse response = reframingService.reframeEmotion(savedLog, therapyMode);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    @Operation(summary = "获取当前用户的所有日志（含分析结果）")
    public ResponseEntity<List<DailyLog>> getLogs(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dailyLogRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId()));
    }
}
