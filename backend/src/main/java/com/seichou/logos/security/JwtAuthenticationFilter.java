package com.seichou.logos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter (JWT 认证过滤器)
 * 拦截每个 HTTP 请求，检查 Header 中是否包含有效的 JWT Token。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. 获取 Authorization Header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. 如果没有 Header 或不是以 Bearer 开头，直接放行（交由后续的 Security 配置决定是否拦截）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 Token (去掉 "Bearer " 前缀)
        jwt = authHeader.substring(7);
        
        // 4. 从 Token 中解析出用户名 (邮箱)
        userEmail = jwtService.extractUsername(jwt);

        // 5. 如果用户名存在且当前 SecurityContext 中没有认证信息
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 从数据库加载用户信息
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            
            // 验证 Token 是否有效
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // 创建认证 Token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 将认证信息存入 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 6. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
}
