package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "adventures")
public class AdventureEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @Column(name = "adventure_type", nullable = false, length = 50)
    private String adventureType;

    @Column(nullable = false)
    private boolean repeatable;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "available_until")
    private Instant availableUntil;

    protected AdventureEntity() {}

    public AdventureEntity(UUID id, String title, String description, String difficulty,
                           int xpReward, String adventureType, boolean repeatable,
                           Instant availableFrom, Instant availableUntil) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.xpReward = xpReward;
        this.adventureType = adventureType;
        this.repeatable = repeatable;
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDifficulty() { return difficulty; }
    public int getXpReward() { return xpReward; }
    public String getAdventureType() { return adventureType; }
    public boolean isRepeatable() { return repeatable; }
    public Instant getAvailableFrom() { return availableFrom; }
    public Instant getAvailableUntil() { return availableUntil; }
}
