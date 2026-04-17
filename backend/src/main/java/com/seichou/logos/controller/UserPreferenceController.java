package com.seichou.logos.controller;

import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserPreferences;
import com.seichou.logos.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preference Controller", description = "用户偏好设置 API")
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "获取当前用户的偏好设置")
    public ResponseEntity<UserPreferences> getPreferences(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferenceService.getPreferences(user.getUserId()));
    }

    @PutMapping
    @Operation(summary = "更新当前用户的偏好设置")
    public ResponseEntity<UserPreferences> updatePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody UserPreferences updates) {
        return ResponseEntity.ok(preferenceService.updatePreferences(user.getUserId(), updates));
    }
}
