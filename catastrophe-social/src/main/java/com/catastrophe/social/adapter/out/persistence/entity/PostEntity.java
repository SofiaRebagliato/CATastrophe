package com.catastrophe.social.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    private UUID id;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "post_type", nullable = false, length = 30)
    private String postType;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostEntity() {}

    public PostEntity(UUID id, UUID catId, String content, String imageUrl,
                      String postType, int likeCount, int commentCount, Instant createdAt) {
        this.id = id;
        this.catId = catId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.postType = postType;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    // ── Getters ──
    public UUID getId() { return id; }
    public UUID getCatId() { return catId; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public String getPostType() { return postType; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public Instant getCreatedAt() { return createdAt; }

    // ── Setters (necesarios para JPA merge) ──
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}
