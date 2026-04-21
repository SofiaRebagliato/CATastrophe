package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cat_badges")
public class CatBadgeEntity {

    @Id
    private UUID id;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private Instant earnedAt;

    protected CatBadgeEntity() {}

    public CatBadgeEntity(UUID id, UUID catId, UUID badgeId, Instant earnedAt) {
        this.id = id;
        this.catId = catId;
        this.badgeId = badgeId;
        this.earnedAt = earnedAt;
    }

    public UUID getId() { return id; }
    public UUID getCatId() { return catId; }
    public UUID getBadgeId() { return badgeId; }
    public Instant getEarnedAt() { return earnedAt; }
}
