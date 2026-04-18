package com.seichou.logos.entity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "vitality_exp", nullable = false)
    @JsonProperty("vitalityExp")
    private Integer vitalityExp = 0;

    @Column(name = "flow_exp", nullable = false)
    @JsonProperty("flowExp")
    private Integer flowExp = 0;

    @Column(name = "spark_exp", nullable = false)
    @JsonProperty("sparkExp")
    private Integer sparkExp = 0;

    @Column(name = "echo_exp", nullable = false)
    @JsonProperty("echoExp")
    private Integer echoExp = 0;

    @Column(name = "resilience_exp", nullable = false)
    @JsonProperty("resilienceExp")
    private Integer resilienceExp = 0;

    @Column(name = "max_exp", nullable = false)
    @JsonProperty("maxExp")
    private Integer maxExp = 100;

    @Column(name = "unlocked_badges", columnDefinition = "TEXT")
    private String unlockedBadgesRaw = "";

    @Transient
    @JsonProperty("unlockedBadges")
    public List<String> getUnlockedBadges() {
        if (unlockedBadgesRaw == null || unlockedBadgesRaw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(unlockedBadgesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void setUnlockedBadges(List<String> badges) {
        this.unlockedBadgesRaw = badges == null || badges.isEmpty()
                ? ""
                : String.join(",", badges);
    }

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    public UserStats() {
    }

    public UserStats(UUID userId, User user, Integer level, Integer vitalityExp, Integer flowExp, Integer sparkExp, Integer echoExp, Integer resilienceExp, Integer maxExp, String unlockedBadgesRaw, java.time.LocalDateTime updatedAt) {
        this.userId = userId;
        this.user = user;
        this.level = level;
        this.vitalityExp = vitalityExp;
        this.flowExp = flowExp;
        this.sparkExp = sparkExp;
        this.echoExp = echoExp;
        this.resilienceExp = resilienceExp;
        this.maxExp = maxExp;
        this.unlockedBadgesRaw = unlockedBadgesRaw;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getVitalityExp() { return vitalityExp; }
    public void setVitalityExp(Integer vitalityExp) { this.vitalityExp = vitalityExp; }
    public Integer getFlowExp() { return flowExp; }
    public void setFlowExp(Integer flowExp) { this.flowExp = flowExp; }
    public Integer getSparkExp() { return sparkExp; }
    public void setSparkExp(Integer sparkExp) { this.sparkExp = sparkExp; }
    public Integer getEchoExp() { return echoExp; }
    public void setEchoExp(Integer echoExp) { this.echoExp = echoExp; }
    public Integer getResilienceExp() { return resilienceExp; }
    public void setResilienceExp(Integer resilienceExp) { this.resilienceExp = resilienceExp; }
    public Integer getMaxExp() { return maxExp; }
    public void setMaxExp(Integer maxExp) { this.maxExp = maxExp; }
    public String getUnlockedBadgesRaw() { return unlockedBadgesRaw; }
    public void setUnlockedBadgesRaw(String unlockedBadgesRaw) { this.unlockedBadgesRaw = unlockedBadgesRaw; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
