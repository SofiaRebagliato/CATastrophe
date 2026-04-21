package com.catastrophe.adventures.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Un reto PvP entre gatos.
 */
public record Challenge(
        UUID id,
        String title,
        String description,
        String challengeType,
        int xpReward,
        Instant startsAt,
        Instant endsAt
) {
    public static final String TYPE_HUNTING = "hunting";
    public static final String TYPE_RACING = "racing";
    public static final String TYPE_NAPPING = "napping";
    public static final String TYPE_GROOMING = "grooming";
}
