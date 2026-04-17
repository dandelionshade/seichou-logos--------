package com.seichou.logos.repository;

import com.seichou.logos.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // 根据用户名（邮箱）查找用户，用于登录认证
    Optional<User> findByUsername(String username);
}
