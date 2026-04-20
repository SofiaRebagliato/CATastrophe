package com.catastrophe.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Relación de seguimiento entre gatos.
 * follower_id sigue a followed_id.
 */
public record Follow(
        UUID id,
        UUID followerId,
        UUID followedId,
        Instant createdAt
) {
    public static Follow create(UUID followerId, UUID followedId) {
        return new Follow(UUID.randomUUID(), followerId, followedId, Instant.now());
    }
}
