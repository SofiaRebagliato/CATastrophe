package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "challenges")
public class ChallengeEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "challenge_type", nullable = false, length = 50)
    private String challengeType;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    protected ChallengeEntity() {}

    public ChallengeEntity(UUID id, String title, String description, String challengeType,
                           int xpReward, Instant startsAt, Instant endsAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.challengeType = challengeType;
        this.xpReward = xpReward;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getChallengeType() { return challengeType; }
    public int getXpReward() { return xpReward; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
}
