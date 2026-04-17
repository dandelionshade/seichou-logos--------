package com.seichou.logos.repository;

import com.seichou.logos.entity.BoardCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardCardRepository extends JpaRepository<BoardCard, UUID> {
    List<BoardCard> findByUser_UserId(UUID userId);
    List<BoardCard> findByUser_UserIdAndStatus(UUID userId, String status);
}
