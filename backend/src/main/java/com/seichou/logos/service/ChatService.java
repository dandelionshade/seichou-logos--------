package com.seichou.logos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seichou.logos.entity.ChatMessage;
import com.seichou.logos.entity.User;
import com.seichou.logos.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.key}")
    private String apiKey;

    public List<ChatMessage> getChatHistory(User user) {
        return repository.findByUserOrderByCreatedAtAsc(user);
    }

    @Transactional
    public ChatMessage sendMessage(User user, String content) {
        // 1. 保存用户消息
        ChatMessage userMessage = ChatMessage.builder()
                .user(user)
                .role("user")
                .content(content)
                .build();
        repository.save(userMessage);

        // 2. 获取历史消息用于上下文 (最近10条)
        List<ChatMessage> history = getChatHistory(user);
        
        // 3. 调用 AI 获取回复
        String aiReply = callAi(user, history);

        // 4. 保存 AI 回复
        ChatMessage assistantMessage = ChatMessage.builder()
                .user(user)
                .role("assistant")
                .content(aiReply)
                .build();
        return repository.save(assistantMessage);
    }

    private String callAi(User user, List<ChatMessage> history) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", 
            "You are a compassionate AI mentor in the Seichou-Logos app. " +
            "Your goal is to help the user grow by reframing their experiences with empathy and wisdom. " +
            "Keep your replies concise and meaningful."));

        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-chat",
                "messages", messages
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Chat AI call failed", e);
            return "I'm sorry, I'm having trouble connecting right now. But I'm still here for you.";
        }
    }
}
