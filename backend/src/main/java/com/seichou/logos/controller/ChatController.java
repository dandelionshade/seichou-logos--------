package com.seichou.logos.controller;

import com.seichou.logos.entity.ChatMessage;
import com.seichou.logos.entity.User;
import com.seichou.logos.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Controller", description = "智聊室 API")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/history")
    @Operation(summary = "获取当前用户的聊天历史")
    public ResponseEntity<List<ChatMessage>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getChatHistory(user));
    }

    @PostMapping("/send")
    @Operation(summary = "向 AI 发送消息并获取回复")
    public ResponseEntity<ChatMessage> sendMessage(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(chatService.sendMessage(user, content));
    }

    @PostMapping
    @Operation(summary = "前端会话聊天接口")
    public ResponseEntity<Map<String, Object>> chat(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request
    ) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.getOrDefault("messages", List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> userContext = (Map<String, Object>) request.getOrDefault("userContext", Map.of());
        String therapyMode = String.valueOf(request.getOrDefault("therapyMode", "adlerian"));

        return ResponseEntity.ok(chatService.chat(user, messages, userContext, therapyMode));
    }
}
