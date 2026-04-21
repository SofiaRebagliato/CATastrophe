package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cat_challenges")
public class CatChallengeEntity {

    @Id
    private UUID id;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Column(name = "opponent_id")
    private UUID opponentId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CatChallengeEntity() {}

    public CatChallengeEntity(UUID id, UUID catId, UUID challengeId, UUID opponentId,
                              String status, int score, Instant createdAt) {
        this.id = id;
        this.catId = catId;
        this.challengeId = challengeId;
        this.opponentId = opponentId;
        this.status = status;
        this.score = score;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getCatId() { return catId; }
    public UUID getChallengeId() { return challengeId; }
    public UUID getOpponentId() { return opponentId; }
    public String getStatus() { return status; }
    public int getScore() { return score; }
    public Instant getCreatedAt() { return createdAt; }
}
