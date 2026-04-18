package com.seichou.logos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seichou.logos.entity.ChatMessage;
import com.seichou.logos.entity.User;
import com.seichou.logos.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ChatService {

    private static final Logger logger = Logger.getLogger(ChatService.class.getName());

    private final ChatMessageRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.key}")
    private String apiKey;

    public ChatService(ChatMessageRepository repository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ChatMessage> getChatHistory(User user) {
        return repository.findByUserOrderByCreatedAtAsc(user);
    }

    public Map<String, Object> chat(List<Map<String, String>> messages, Map<String, Object> userContext, String therapyMode) {
        String reply = callAi(messages, userContext, therapyMode);
        Map<String, Object> response = new HashMap<>();
        response.put("reply", reply);
        response.put("action", null);
        return response;
    }

    @Transactional
    public ChatMessage sendMessage(User user, String content) {
        // 1. 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setUser(user);
        userMessage.setRole("user");
        userMessage.setContent(content);
        repository.save(userMessage);

        // 2. 获取历史消息用于上下文 (最近10条)
        List<ChatMessage> history = getChatHistory(user);
        
        // 3. 调用 AI 获取回复
        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage messageItem : history) {
            Map<String, String> message = new HashMap<>();
            message.put("role", messageItem.getRole());
            message.put("content", messageItem.getContent());
            messages.add(message);
        }
        String aiReply = callAi(messages, Map.of("userId", user.getUserId().toString()), "adlerian");

        // 4. 保存 AI 回复
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setUser(user);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(aiReply);
        return repository.save(assistantMessage);
    }

    private String callAi(List<Map<String, String>> history, Map<String, Object> userContext, String therapyMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
            "You are a compassionate AI mentor in the Seichou-Logos app. " +
            "Therapy mode: " + therapyMode + ". " +
            "Current user context: " + userContext + ". " +
            "Your goal is to help the user grow by reframing their experiences with empathy and wisdom. " +
            "Keep your replies concise and meaningful.");
        messages.add(systemMessage);

        messages.addAll(history);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Chat AI call failed", e);
            return "I'm sorry, I'm having trouble connecting right now. But I'm still here for you.";
        }
    }
}
