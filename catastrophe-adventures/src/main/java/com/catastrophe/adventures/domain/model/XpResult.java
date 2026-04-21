package com.catastrophe.adventures.domain.model;

/**
 * Resultado del cálculo de XP y nivel tras ganar experiencia.
 *
 * Los umbrales de nivel siguen una curva cuadrática:
 * Nivel N requiere N * N * 100 XP totales.
 */
public record XpResult(
        int totalXp,
        int level,
        boolean leveledUp
) {
    /** Umbral de XP para alcanzar un nivel dado. */
    public static int xpForLevel(int level) {
        return level * level * 100;
    }

    /** Calcular nuevo nivel y XP tras ganar experiencia. */
    public static XpResult calculate(int currentXp, int currentLevel, int xpGained) {
        int newXp = currentXp + xpGained;
        int newLevel = currentLevel;

        // Subir de nivel mientras haya XP suficiente
        while (newXp >= xpForLevel(newLevel + 1)) {
            newLevel++;
        }

        return new XpResult(newXp, newLevel, newLevel > currentLevel);
    }
}
