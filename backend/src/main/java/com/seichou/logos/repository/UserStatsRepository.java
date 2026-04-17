package com.seichou.logos.repository;

import com.seichou.logos.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {
    Optional<UserStats> findByUser_UserId(UUID userId);
}
