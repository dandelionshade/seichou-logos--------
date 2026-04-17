package com.seichou.logos.controller;

import com.seichou.logos.entity.BoardCard;
import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserStats;
import com.seichou.logos.repository.BoardCardRepository;
import com.seichou.logos.repository.UserStatsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
@Tag(name = "Board Controller", description = "Kanban Board & User Stats API")
public class BoardController {

    private final BoardCardRepository boardCardRepository;
    private final UserStatsRepository userStatsRepository;

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
        card.setStatus("todo");
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

    @GetMapping("/stats")
    @Operation(summary = "Get user stats (level, exp)")
    public ResponseEntity<UserStats> getUserStats(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        return userStatsRepository.findByUser_UserId(user.getUserId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/settle")
    @Operation(summary = "Settle EXP from done cards")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> settleExp(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {
        
        // 1. Get all 'done' cards for this user
        List<BoardCard> doneCards = boardCardRepository.findByUser_UserIdAndStatus(user.getUserId(), "done");
        
        if (doneCards.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "physical_exp", 0,
                "mental_exp", 0,
                "compassionate_summary", "No completed tasks to settle today. Every small step counts!"
            ));
        }

        // 2. Calculate EXP (Simple logic for now: 10 EXP per card)
        int physicalExpGain = (int) doneCards.stream().filter(c -> "physical".equals(c.getDimension())).count() * 10;
        int mentalExpGain = (int) doneCards.stream().filter(c -> "mental".equals(c.getDimension())).count() * 10;

        // 3. Update UserStats
        UserStats stats = userStatsRepository.findByUser_UserId(user.getUserId()).orElseThrow();
        stats.setPhysicalExp(stats.getPhysicalExp() + physicalExpGain);
        stats.setMentalExp(stats.getMentalExp() + mentalExpGain);

        // Level up logic
        int totalExp = stats.getPhysicalExp() + stats.getMentalExp();
        if (totalExp >= stats.getMaxExp()) {
            stats.setLevel(stats.getLevel() + 1);
            stats.setMaxExp((int) (stats.getMaxExp() * 1.5));
        }
        userStatsRepository.save(stats);

        // 4. Delete settled cards (or mark as settled/archived)
        boardCardRepository.deleteAll(doneCards);

        return ResponseEntity.ok(Map.of(
                "physical_exp", physicalExpGain,
                "mental_exp", mentalExpGain,
                "compassionate_summary", "You've made significant progress! Your efforts are being woven into the tapestry of your growth."
        ));
    }
}
