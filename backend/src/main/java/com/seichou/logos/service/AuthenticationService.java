package com.seichou.logos.service;

import com.seichou.logos.dto.AuthenticationRequest;
import com.seichou.logos.dto.AuthenticationResponse;
import com.seichou.logos.dto.AuthUserDto;
import com.seichou.logos.dto.RegisterRequest;
import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserPreferences;
import com.seichou.logos.entity.UserStats;
import com.seichou.logos.repository.UserRepository;
import com.seichou.logos.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthenticationService (认证服务)
 * 处理用户注册和登录的业务逻辑。
 */
@Service
public class AuthenticationService {

    private final UserRepository repository;
    private final com.seichou.logos.repository.UserStatsRepository userStatsRepository;
    private final com.seichou.logos.repository.UserPreferencesRepository userPreferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

        public AuthenticationService(
                        UserRepository repository,
                        com.seichou.logos.repository.UserStatsRepository userStatsRepository,
                        com.seichou.logos.repository.UserPreferencesRepository userPreferencesRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AuthenticationManager authenticationManager
        ) {
                this.repository = repository;
                this.userStatsRepository = userStatsRepository;
                this.userPreferencesRepository = userPreferencesRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.authenticationManager = authenticationManager;
        }

    // 处理用户注册
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. 创建 User 实体，密码需要加密
        User user = new User();
        user.setUsername(request.getEmail());
        if (request.getEmail() != null && request.getEmail().contains("@")) {
            user.setDisplayName(request.getEmail().split("@")[0]);
        } else {
            user.setDisplayName("Seichou User");
        }
        user.setBio("A passionate learner on a journey of growth.");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        
        // 2. 保存到数据库
        User savedUser = repository.save(user);

        // 3. 初始化用户数值 (UserStats)
        UserStats stats = new UserStats();
        stats.setUser(savedUser);
        stats.setLevel(1);
        stats.setVitalityExp(0);
        stats.setFlowExp(0);
        stats.setSparkExp(0);
        stats.setEchoExp(0);
        stats.setResilienceExp(0);
        stats.setMaxExp(100);
        userStatsRepository.save(stats);

        // 4. 初始化用户偏好 (UserPreferences)
        UserPreferences prefs = new UserPreferences();
        prefs.setUser(savedUser);
        prefs.setTheme("dark");
        prefs.setLanguage("en");
        prefs.setAiPersonality("empathetic");
        prefs.setNotificationsEnabled(true);
        prefs.setDataPrivacy("standard");
        userPreferencesRepository.save(prefs);
        
        // 5. 生成 JWT Token
        String jwtToken = jwtService.generateToken(savedUser);
        
        return new AuthenticationResponse(jwtToken, toAuthUser(savedUser));
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
        User user = repository.findByUsername(request.getEmail()).orElseThrow();
                
        // 3. 生成 JWT Token
        String jwtToken = jwtService.generateToken(user);
        
        return new AuthenticationResponse(jwtToken, toAuthUser(user));
    }

    private AuthUserDto toAuthUser(User user) {
        return new AuthUserDto(
            user.getUserId().toString(),
            user.getUsername(),
            user.getDisplayName(),
            user.getBio(),
            user.getRole() != null ? user.getRole().name() : null,
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
