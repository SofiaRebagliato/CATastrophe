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
     * Umbral de XP total acumulado necesario para alcanzar un nivel dado.
     * Curva cuadrática: nivel N requiere N² · 100 XP en total.
     * <p>
     * Coherente con {@code XpResult} en {@code catastrophe-adventures} —
     * ambos servicios deben usar la misma fórmula para que la cadena
     * de eventos XP sea consistente.
     */
    public static int xpForLevel(int level) {
        return level * level * 100;
    }

    /**
     * Calcula el XP necesario para alcanzar el siguiente nivel.
     */
    public int xpForNextLevel() {
        return xpForLevel(level + 1);
    }

    /**
     * Comprueba si el gato puede subir de nivel con su XP actual.
     */
    public boolean canLevelUp() {
        return xp >= xpForNextLevel();
    }

    /**
     * Añade XP y sube de nivel si corresponde. {@code xp} representa
     * el total acumulado (no residual): a mayor xp, mayor progreso.
     * Devuelve una nueva instancia (inmutabilidad).
     */
    public Cat addXp(int amount) {
        int newXp = this.xp + amount;
        int newLevel = this.level;

        while (newXp >= xpForLevel(newLevel + 1)) {
            newLevel++;
        }

        return new Cat(id, humanId, name, breed, ageMonths, avatarUrl, bio,
                newXp, newLevel, mood, createdAt, Instant.now());
    }
}
