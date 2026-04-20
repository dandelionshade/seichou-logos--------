package com.seichou.logos.controller;

import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserPreferences;
import com.seichou.logos.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/preferences")
@Tag(name = "User Preference Controller", description = "用户偏好设置 API")
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    public UserPreferenceController(UserPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    @Operation(summary = "获取当前用户的偏好设置")
    public ResponseEntity<Map<String, Object>> getPreferences(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(preferenceService.getPreferences(user.getUserId())));
    }

    @PutMapping
    @Operation(summary = "更新当前用户的偏好设置")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody UserPreferences updates) {
        UserPreferences saved = preferenceService.updatePreferences(user.getUserId(), updates);
        return ResponseEntity.ok(toResponse(saved));
    }

    private Map<String, Object> toResponse(UserPreferences prefs) {
        boolean hasDeepseekApiKey = prefs.getDeepseekApiKey() != null && !prefs.getDeepseekApiKey().isBlank();
        return Map.of(
                "theme", prefs.getTheme(),
                "language", prefs.getLanguage(),
                "aiPersonality", prefs.getAiPersonality(),
                "notificationsEnabled", prefs.getNotificationsEnabled(),
                "dataPrivacy", prefs.getDataPrivacy(),
                "hasDeepseekApiKey", hasDeepseekApiKey
        );
    }
}
