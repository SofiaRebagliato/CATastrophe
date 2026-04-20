package com.catastrophe.profiles.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Un gato (el verdadero usuario de la plataforma).
 *
 * Record inmutable con métodos de negocio para XP, niveles y avatares.
 */
public record Cat(
        UUID id,
        UUID humanId,
        String name,
        String breed,
        Integer ageMonths,
        String avatarUrl,
        String bio,
        int xp,
        int level,
        String mood,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Factory method para crear un nuevo gato con valores por defecto.
     */
    public static Cat create(UUID humanId, String name, String breed, Integer ageMonths, String bio) {
        return new Cat(
                UUID.randomUUID(),
                humanId,
                name,
                breed,
                ageMonths,
                null, // avatarUrl se asigna después
                bio,
                0,    // xp
                1,    // level
                "curious", // mood por defecto
                Instant.now(),
                null
        );
    }

    /**
     * Devuelve una copia con un nuevo avatar.
     */
    public Cat withAvatar(String avatarUrl) {
        return new Cat(id, humanId, name, breed, ageMonths, avatarUrl, bio, xp, level, mood, createdAt, Instant.now());
    }

    /**
     * Calcula el XP necesario para el siguiente nivel.
     * Fórmula progresiva: nivel * 100.
     */
    public int xpForNextLevel() {
        return level * 100;
    }

    /**
     * Comprueba si el gato puede subir de nivel.
     */
    public boolean canLevelUp() {
        return xp >= xpForNextLevel();
    }

    /**
     * Añade XP y sube de nivel si corresponde.
     * Devuelve una nueva instancia (inmutabilidad).
     */
    public Cat addXp(int amount) {
        int newXp = this.xp + amount;
        int newLevel = this.level;

        while (newXp >= newLevel * 100) {
            newXp -= newLevel * 100;
            newLevel++;
        }

        return new Cat(id, humanId, name, breed, ageMonths, avatarUrl, bio, newXp, newLevel, mood, createdAt, Instant.now());
    }
}
