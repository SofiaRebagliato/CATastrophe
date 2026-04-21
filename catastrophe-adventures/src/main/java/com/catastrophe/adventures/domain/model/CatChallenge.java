package com.catastrophe.adventures.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Participación de un gato en un reto PvP.
 */
public record CatChallenge(
        UUID id,
        UUID catId,
        UUID challengeId,
        UUID opponentId,
        ChallengeStatus status,
        int score,
        Instant createdAt
) {
    public enum ChallengeStatus {
        PENDING, ACTIVE, WON, LOST, DRAW;

        public String dbValue() {
            return name().toLowerCase();
        }

        public static ChallengeStatus fromDb(String value) {
            return valueOf(value.toUpperCase());
        }
    }

    /** Crear una participación nueva (esperando rival). */
    public static CatChallenge create(UUID catId, UUID challengeId) {
        return new CatChallenge(
                UUID.randomUUID(), catId, challengeId,
                null, ChallengeStatus.PENDING, 0, Instant.now()
        );
    }

    /** Aceptar un reto (asignar oponente). */
    public CatChallenge accept(UUID opponentId) {
        return new CatChallenge(id, catId, challengeId, opponentId,
                ChallengeStatus.ACTIVE, score, createdAt);
    }

    /** Resolver con puntuación. */
    public CatChallenge resolve(ChallengeStatus result, int finalScore) {
        return new CatChallenge(id, catId, challengeId, opponentId,
                result, finalScore, createdAt);
    }
}
