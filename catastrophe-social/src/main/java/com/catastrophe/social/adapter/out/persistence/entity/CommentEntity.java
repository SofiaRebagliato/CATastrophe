package com.catastrophe.social.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class CommentEntity {

    @Id
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CommentEntity() {}

    public CommentEntity(UUID id, UUID postId, UUID catId, String content, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.catId = catId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getPostId() { return postId; }
    public UUID getCatId() { return catId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
