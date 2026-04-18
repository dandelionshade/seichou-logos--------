package com.seichou.logos.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "board_cards")
public class BoardCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_id", updatable = false, nullable = false)
    @JsonIgnore
    private UUID cardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "time_frame", nullable = false)
    @JsonIgnore
    private String timeFrame; // today, week, month, longterm

    @Column(nullable = false)
    @JsonIgnore
    private String dimension; // physical, mental

    @ElementCollection
    @CollectionTable(name = "board_card_tags", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection
    @CollectionTable(name = "board_card_checkpoints", joinColumns = @JoinColumn(name = "card_id"))
    private List<Checkpoint> checkpoints;

    @Column(precision = 5, scale = 2)
    private Double progress; // 0-100

    @Column(nullable = false)
    private String status; // todo, done

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public BoardCard() {
    }

    public BoardCard(UUID cardId, User user, String title, String description, String timeFrame, String dimension, List<String> tags, List<Checkpoint> checkpoints, Double progress, String status, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.user = user;
        this.title = title;
        this.description = description;
        this.timeFrame = timeFrame;
        this.dimension = dimension;
        this.tags = tags;
        this.checkpoints = checkpoints;
        this.progress = progress;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static BoardCardBuilder builder() { return new BoardCardBuilder(); }

    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimeFrame() { return timeFrame; }
    public void setTimeFrame(String timeFrame) { this.timeFrame = timeFrame; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<Checkpoint> getCheckpoints() { return checkpoints; }
    public void setCheckpoints(List<Checkpoint> checkpoints) { this.checkpoints = checkpoints; }
    public Double getProgress() { return progress; }
    public void setProgress(Double progress) { this.progress = progress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @JsonProperty("id")
    public String getId() {
        return cardId != null ? cardId.toString() : null;
    }

    @JsonProperty("id")
    public void setId(String id) {
        if (id != null && !id.isBlank()) {
            this.cardId = UUID.fromString(id);
        }
    }

    @JsonProperty("category")
    public String getCategory() {
        return dimension;
    }

    @JsonProperty("category")
    public void setCategory(String category) {
        this.dimension = category;
    }

    public static class BoardCardBuilder {
        private UUID cardId;
        private User user;
        private String title;
        private String description;
        private String timeFrame;
        private String dimension;
        private List<String> tags;
        private List<Checkpoint> checkpoints;
        private Double progress;
        private String status;
        private LocalDateTime createdAt;

        public BoardCardBuilder cardId(UUID cardId) { this.cardId = cardId; return this; }
        public BoardCardBuilder user(User user) { this.user = user; return this; }
        public BoardCardBuilder title(String title) { this.title = title; return this; }
        public BoardCardBuilder description(String description) { this.description = description; return this; }
        public BoardCardBuilder timeFrame(String timeFrame) { this.timeFrame = timeFrame; return this; }
        public BoardCardBuilder dimension(String dimension) { this.dimension = dimension; return this; }
        public BoardCardBuilder tags(List<String> tags) { this.tags = tags; return this; }
        public BoardCardBuilder checkpoints(List<Checkpoint> checkpoints) { this.checkpoints = checkpoints; return this; }
        public BoardCardBuilder progress(Double progress) { this.progress = progress; return this; }
        public BoardCardBuilder status(String status) { this.status = status; return this; }
        public BoardCardBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public BoardCard build() {
            return new BoardCard(cardId, user, title, description, timeFrame, dimension, tags, checkpoints, progress, status, createdAt);
        }
    }

    @PrePersist
    public void applyDefaults() {
        if (status == null || status.isBlank()) {
            status = "todo";
        }
        if (timeFrame == null || timeFrame.isBlank()) {
            timeFrame = "today";
        }
        if (dimension == null || dimension.isBlank()) {
            dimension = "mind";
        }
    }
}
