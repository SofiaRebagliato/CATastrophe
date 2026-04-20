package com.catastrophe.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Un "like" en un post.
 * Restricción: un gato solo puede dar un like por post.
 */
public record Like(
        UUID id,
        UUID postId,
        UUID catId,
        Instant createdAt
) {
    public static Like create(UUID postId, UUID catId) {
        return new Like(UUID.randomUUID(), postId, catId, Instant.now());
    }
}
