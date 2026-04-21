package com.catastrophe.adventures.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Badge ganado por un gato.
 */
public record CatBadge(
        UUID id,
        UUID catId,
        UUID badgeId,
        Instant earnedAt
) {
    public static CatBadge award(UUID catId, UUID badgeId) {
        return new CatBadge(UUID.randomUUID(), catId, badgeId, Instant.now());
    }
}
