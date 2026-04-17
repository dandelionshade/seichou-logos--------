package com.seichou.logos.service;

import com.seichou.logos.dto.AuthenticationRequest;
import com.seichou.logos.dto.AuthenticationResponse;
import com.seichou.logos.dto.RegisterRequest;
import com.seichou.logos.entity.User;
import com.seichou.logos.repository.UserRepository;
import com.seichou.logos.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthenticationService (认证服务)
 * 处理用户注册和登录的业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final com.seichou.logos.repository.UserStatsRepository userStatsRepository;
    private final com.seichou.logos.repository.UserPreferencesRepository userPreferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // 处理用户注册
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. 创建 User 实体，密码需要加密
        var user = User.builder()
                .username(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER) // 默认注册为普通用户
                .build();
        
        // 2. 保存到数据库
        var savedUser = repository.save(user);

        // 3. 初始化用户数值 (UserStats)
        var stats = com.seichou.logos.entity.UserStats.builder()
                .user(savedUser)
                .level(1)
                .physicalExp(0)
                .mentalExp(0)
                .maxExp(100)
                .build();
        userStatsRepository.save(stats);

        // 4. 初始化用户偏好 (UserPreferences)
        var prefs = com.seichou.logos.entity.UserPreferences.builder()
                .user(savedUser)
                .theme("dark")
                .language("en")
                .aiPersonality("empathetic")
                .notificationsEnabled(true)
                .dataPrivacy("standard")
                .build();
        userPreferencesRepository.save(prefs);
        
        // 5. 生成 JWT Token
        var jwtToken = jwtService.generateToken(savedUser);
        
        // 6. 返回 Token
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    // 处理用户登录
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 1. 使用 AuthenticationManager 进行认证 (底层会调用 UserDetailsService 和 PasswordEncoder)
        // 如果密码错误，这里会抛出 BadCredentialsException，被全局异常处理器捕获
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        // 2. 认证通过后，从数据库获取用户信息
        var user = repository.findByUsername(request.getEmail())
                .orElseThrow();
                
        // 3. 生成 JWT Token
        var jwtToken = jwtService.generateToken(user);
        
        // 4. 返回 Token
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
