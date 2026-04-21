package com.catastrophe.adventures.domain.model;

import java.util.UUID;

/**
 * Modelo de dominio — Una insignia/logro coleccionable.
 */
public record Badge(
        UUID id,
        String name,
        String description,
        String iconUrl,
        String rarity
) {
    public static final String RARITY_COMMON = "common";
    public static final String RARITY_RARE = "rare";
    public static final String RARITY_EPIC = "epic";
    public static final String RARITY_LEGENDARY = "legendary";
}
