package com.seichou.logos.repository;

import com.seichou.logos.entity.ChatMessage;
import com.seichou.logos.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByUserOrderByCreatedAtAsc(User user);
}
