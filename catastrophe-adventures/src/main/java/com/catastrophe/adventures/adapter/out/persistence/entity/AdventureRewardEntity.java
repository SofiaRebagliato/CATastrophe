package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "adventure_rewards")
public class AdventureRewardEntity {

    @Id
    private UUID id;

    @Column(name = "adventure_id", nullable = false)
    private UUID adventureId;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    protected AdventureRewardEntity() {}

    public UUID getId() { return id; }
    public UUID getAdventureId() { return adventureId; }
    public UUID getBadgeId() { return badgeId; }
}
