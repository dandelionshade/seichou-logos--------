package com.seichou.logos.controller;

import com.seichou.logos.entity.User;
import com.seichou.logos.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UserController (用户控制器)
 * 用于处理用户个人主页、偏好设置等功能。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "用户个人信息与设置 API")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        return ResponseEntity.ok(Map.of(
                "id", user.getUserId().toString(),
                "email", user.getUsername(),
                "role", user.getRole().name(),
                "joinedAt", user.getCreatedAt().toString()
        ));
    }

    @PutMapping("/me/password")
    @Operation(summary = "修改当前用户密码")
    public ResponseEntity<Map<String, Object>> updatePassword(@RequestBody Map<String, String> request) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing passwords"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Incorrect current password"));
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @GetMapping("/settings")
    @Operation(summary = "获取用户偏好设置")
    public ResponseEntity<Map<String, Object>> getUserSettings() {
        // TODO: 真实逻辑 - 从数据库获取用户的偏好设置 (需要新建 Settings 实体)
        return ResponseEntity.ok(Map.of(
                "theme", "dark",
                "notificationsEnabled", true,
                "language", "zh-CN"
        ));
    }
}
