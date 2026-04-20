package com.catastrophe.social.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follows")
public class FollowEntity {

    @Id
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "followed_id", nullable = false)
    private UUID followedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FollowEntity() {}

    public FollowEntity(UUID id, UUID followerId, UUID followedId, Instant createdAt) {
        this.id = id;
        this.followerId = followerId;
        this.followedId = followedId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getFollowerId() { return followerId; }
    public UUID getFollowedId() { return followedId; }
    public Instant getCreatedAt() { return createdAt; }
}
