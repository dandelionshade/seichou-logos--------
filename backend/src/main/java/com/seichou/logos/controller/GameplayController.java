package com.seichou.logos.controller;

import com.seichou.logos.entity.BoardCard;
import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserStats;
import com.seichou.logos.repository.BoardCardRepository;
import com.seichou.logos.repository.UserStatsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Gameplay", description = "Settlement and gamification APIs")
public class GameplayController {

    private final UserStatsRepository userStatsRepository;
    private final BoardCardRepository boardCardRepository;
    private final BoardController boardController;

    public GameplayController(UserStatsRepository userStatsRepository, BoardCardRepository boardCardRepository, BoardController boardController) {
        this.userStatsRepository = userStatsRepository;
        this.boardCardRepository = boardCardRepository;
        this.boardController = boardController;
    }

    @PostMapping("/settle")
    @Operation(summary = "Settle EXP from completed/struggled cards")
    public ResponseEntity<Map<String, Object>> settle(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request
    ) {
        UserStats stats = boardController.getOrCreateStats(user);
        String feeling = String.valueOf(request.getOrDefault("feeling", "smooth"));

        int vitalityGain = 0;
        int flowGain = 0;
        int sparkGain = 0;
        int echoGain = 0;
        int resilienceGain = 0;

        Object cardsRaw = request.get("cards");
        if (cardsRaw instanceof List<?>) {
            List<?> cards = (List<?>) cardsRaw;
            for (Object cardRaw : cards) {
                if (!(cardRaw instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> m = (Map<?, ?>) cardRaw;
                Object categoryValue = m.containsKey("category") ? m.get("category") : "mind";
                Object statusValue = m.containsKey("status") ? m.get("status") : "done";
                String category = String.valueOf(categoryValue);
                String status = String.valueOf(statusValue);

                int base;
                if ("done".equals(status)) {
                    base = 12;
                } else if ("struggled".equals(status)) {
                    base = 7;
                } else if ("composted".equals(status)) {
                    base = 4;
                } else {
                    base = 5;
                }

                if ("health".equals(category)) {
                    vitalityGain += base;
                } else if ("mind".equals(category)) {
                    flowGain += base;
                } else if ("career".equals(category)) {
                    sparkGain += base;
                } else if ("social".equals(category)) {
                    echoGain += base;
                } else {
                    flowGain += base;
                }

                if ("struggled".equals(status)) {
                    resilienceGain += 6;
                }
                if ("composted".equals(status)) {
                    resilienceGain += 10;
                }

                Object idValue = m.get("id");
                if (idValue != null) {
                    String id = String.valueOf(idValue);
                    try {
                        boardCardRepository.findById(java.util.UUID.fromString(id)).ifPresent(card -> {
                            if (card.getUser().getUserId().equals(user.getUserId())) {
                                boardCardRepository.delete(card);
                            }
                        });
                    } catch (IllegalArgumentException ignoredInvalidUuid) {
                        // Ignore client-side temporary IDs.
                    }
                }
            }
        }

        if ("struggled".equals(feeling)) {
            resilienceGain += 6;
        }
        if ("composted".equals(feeling)) {
            resilienceGain += 10;
        }

        stats.setVitalityExp(stats.getVitalityExp() + vitalityGain);
        stats.setFlowExp(stats.getFlowExp() + flowGain);
        stats.setSparkExp(stats.getSparkExp() + sparkGain);
        stats.setEchoExp(stats.getEchoExp() + echoGain);
        stats.setResilienceExp(stats.getResilienceExp() + resilienceGain);

        int total = stats.getVitalityExp() + stats.getFlowExp() + stats.getSparkExp() + stats.getEchoExp() + stats.getResilienceExp();
        if (total >= stats.getMaxExp()) {
            stats.setLevel(stats.getLevel() + 1);
            stats.setMaxExp((int) Math.round(stats.getMaxExp() * 1.35));
        }

        List<String> badges = new ArrayList<>(stats.getUnlockedBadges());
        List<String> newlyUnlocked = new ArrayList<>();
        unlockIfAbsent(badges, newlyUnlocked, "resilience_init", stats.getResilienceExp() >= 30);
        unlockIfAbsent(badges, newlyUnlocked, "flow_master", stats.getFlowExp() >= 100);
        unlockIfAbsent(badges, newlyUnlocked, "social_butterfly", stats.getEchoExp() >= 50);
        unlockIfAbsent(badges, newlyUnlocked, "early_bird", stats.getVitalityExp() >= 60);
        stats.setUnlockedBadges(badges);

        userStatsRepository.save(stats);

        Map<String, Object> response = new HashMap<>();
        response.put("vitality_exp", vitalityGain);
        response.put("flow_exp", flowGain);
        response.put("spark_exp", sparkGain);
        response.put("echo_exp", echoGain);
        response.put("resilience_exp", resilienceGain);
        response.put("compassionate_summary", buildSummary(feeling, newlyUnlocked));
        response.put("newlyUnlockedBadges", newlyUnlocked);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/low-battery")
    @Operation(summary = "Low battery mode settlement")
    public ResponseEntity<Map<String, Object>> lowBattery(@AuthenticationPrincipal User user) {
        UserStats stats = boardController.getOrCreateStats(user);
        stats.setResilienceExp(stats.getResilienceExp() + 10);
        userStatsRepository.save(stats);

        return ResponseEntity.ok(Map.of(
                "expGained", 10,
                "message", "今天先休息也很好。你不是退步，而是在为下一次出发积蓄能量。"
        ));
    }

    @PostMapping("/reports/weekly")
    @Operation(summary = "Generate weekly markdown report")
    public ResponseEntity<Map<String, Object>> weeklyReport(@RequestBody(required = false) Map<String, Object> ignored) {
        String markdown = "### Weekly Soul Profile\n\n"
            + "**Theme Color:** Amber Green\n\n"
            + "**Gentle Reminder:** 你并不是执行力不足，而是对自己要求过高。请把目标再切小一步。\n\n"
            + "**Highlight:** 你在疲惫状态下仍然记录并复盘，这本身就是高质量的成长行为。";
        return ResponseEntity.ok(Map.of("markdown", markdown));
    }

    private void unlockIfAbsent(List<String> badges, List<String> newlyUnlocked, String badge, boolean condition) {
        if (condition && !badges.contains(badge)) {
            badges.add(badge);
            newlyUnlocked.add(badge);
        }
    }

    private String buildSummary(String feeling, List<String> newlyUnlocked) {
        String base;
        if ("struggled".equals(feeling)) {
            base = "你在困难中依然推进了进度，这份韧性非常珍贵。";
        } else if ("composted".equals(feeling)) {
            base = "允许低谷发生并不意味着失败，你正在把挫折转化为土壤。";
        } else {
            base = "今天的行动已经沉淀为真实成长，请继续保持节奏。";
        }
        if (newlyUnlocked.isEmpty()) {
            return base;
        }
        return base + " 新解锁徽章: " + String.join(", ", newlyUnlocked) + "。";
    }
}
