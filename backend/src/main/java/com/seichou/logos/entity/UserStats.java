package com.seichou.logos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "physical_exp", nullable = false)
    @Builder.Default
    private Integer physicalExp = 0;

    @Column(name = "mental_exp", nullable = false)
    @Builder.Default
    private Integer mentalExp = 0;

    @Column(name = "max_exp", nullable = false)
    @Builder.Default
    private Integer maxExp = 100;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
