package com.catastrophe.adventures.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Una aventura/misión disponible para los gatos.
 */
public record Adventure(
        UUID id,
        String title,
        String description,
        String difficulty,
        int xpReward,
        String adventureType,
        boolean repeatable,
        Instant availableFrom,
        Instant availableUntil
) {
    public static final String DIFFICULTY_EASY = "easy";
    public static final String DIFFICULTY_MEDIUM = "medium";
    public static final String DIFFICULTY_HARD = "hard";
    public static final String DIFFICULTY_LEGENDARY = "legendary";

    public static final String TYPE_DAILY = "daily";
    public static final String TYPE_WEEKLY = "weekly";
    public static final String TYPE_SPECIAL = "special";

    /** Comprueba si la aventura está activa en el momento dado. */
    public boolean isAvailableAt(Instant now) {
        if (availableFrom != null && now.isBefore(availableFrom)) return false;
        if (availableUntil != null && now.isAfter(availableUntil)) return false;
        return true;
    }
}
