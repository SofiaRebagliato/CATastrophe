package com.catastrophe.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Un comentario en un post.
 */
public record Comment(
        UUID id,
        UUID postId,
        UUID catId,
        String content,
        Instant createdAt
) {
    public static Comment create(UUID postId, UUID catId, String content) {
        return new Comment(UUID.randomUUID(), postId, catId, content, Instant.now());
    }
}
