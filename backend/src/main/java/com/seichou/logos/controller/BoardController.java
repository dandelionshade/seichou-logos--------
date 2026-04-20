package com.seichou.logos.controller;

import com.seichou.logos.entity.BoardCard;
import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserStats;
import com.seichou.logos.repository.BoardCardRepository;
import com.seichou.logos.repository.UserStatsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/board")
@Tag(name = "Board Controller", description = "Kanban Board & User Stats API")
public class BoardController {

    private final BoardCardRepository boardCardRepository;
    private final UserStatsRepository userStatsRepository;

    public BoardController(BoardCardRepository boardCardRepository, UserStatsRepository userStatsRepository) {
        this.boardCardRepository = boardCardRepository;
        this.userStatsRepository = userStatsRepository;
    }

    @GetMapping("/cards")
    @Operation(summary = "Get all board cards for current user")
    public ResponseEntity<List<BoardCard>> getCards(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        return ResponseEntity.ok(boardCardRepository.findByUser_UserId(user.getUserId()));
    }

    @PostMapping("/cards")
    @Operation(summary = "Create a new board card")
    public ResponseEntity<BoardCard> createCard(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user,
            @RequestBody BoardCard card) {
        card.setUser(user);
        return ResponseEntity.ok(boardCardRepository.save(card));
    }

    @PutMapping("/cards/{cardId}")
    @Operation(summary = "Update an existing board card")
    public ResponseEntity<BoardCard> updateCard(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user,
            @PathVariable UUID cardId, 
            @RequestBody BoardCard updates) {
        return boardCardRepository.findById(cardId).map(card -> {
            // Security check: ensure card belongs to user
            if (!card.getUser().getUserId().equals(user.getUserId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).<BoardCard>build();
            }
            if (updates.getTitle() != null) card.setTitle(updates.getTitle());
            if (updates.getDescription() != null) card.setDescription(updates.getDescription());
            if (updates.getTimeFrame() != null) card.setTimeFrame(updates.getTimeFrame());
            if (updates.getDimension() != null) card.setDimension(updates.getDimension());
            if (updates.getTags() != null) card.setTags(updates.getTags());
            if (updates.getCheckpoints() != null) card.setCheckpoints(updates.getCheckpoints());
            if (updates.getProgress() != null) card.setProgress(updates.getProgress());
            if (updates.getStatus() != null) card.setStatus(updates.getStatus());
            return ResponseEntity.ok(boardCardRepository.save(card));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "Delete a board card")
    public ResponseEntity<Void> deleteCard(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user,
            @PathVariable UUID cardId) {
        Optional<BoardCard> cardOptional = boardCardRepository.findById(cardId);
        if (cardOptional.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }

        BoardCard card = cardOptional.get();
        if (!card.getUser().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        boardCardRepository.delete(card);
        return ResponseEntity.status(org.springframework.http.HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user stats (level, exp)")
    public ResponseEntity<UserStats> getUserStats(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        return ResponseEntity.ok(getOrCreateStats(user));
    }

    public UserStats getOrCreateStats(User user) {
        Optional<UserStats> existing = userStatsRepository.findByUser_UserId(user.getUserId());
        if (existing.isPresent()) {
            UserStats stats = existing.get();
            if (stats.getVitalityExp() == null) stats.setVitalityExp(0);
            if (stats.getFlowExp() == null) stats.setFlowExp(0);
            if (stats.getSparkExp() == null) stats.setSparkExp(0);
            if (stats.getEchoExp() == null) stats.setEchoExp(0);
            if (stats.getResilienceExp() == null) stats.setResilienceExp(0);
            if (stats.getMaxExp() == null) stats.setMaxExp(100);
            if (stats.getLevel() == null) stats.setLevel(1);
            if (stats.getUnlockedBadgesRaw() == null) stats.setUnlockedBadgesRaw("");
            return userStatsRepository.save(stats);
        }

        UserStats stats = new UserStats();
        stats.setUser(user);
        stats.setUserId(user.getUserId());
        stats.setLevel(1);
        stats.setVitalityExp(0);
        stats.setFlowExp(0);
        stats.setSparkExp(0);
        stats.setEchoExp(0);
        stats.setResilienceExp(0);
        stats.setMaxExp(100);
        stats.setUnlockedBadgesRaw("");
        return userStatsRepository.save(stats);
    }
}
