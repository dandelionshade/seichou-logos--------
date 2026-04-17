package com.seichou.logos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "board_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_id", updatable = false, nullable = false)
    private UUID cardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "time_frame", nullable = false)
    private String timeFrame; // today, week, month, longterm

    @Column(nullable = false)
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
}
