package com.catastrophe.social.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "likes")
public class LikeEntity {

    @Id
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LikeEntity() {}

    public LikeEntity(UUID id, UUID postId, UUID catId, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.catId = catId;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getPostId() { return postId; }
    public UUID getCatId() { return catId; }
    public Instant getCreatedAt() { return createdAt; }
}
