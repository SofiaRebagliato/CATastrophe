package com.catastrophe.adventures.domain.model;

import com.catastrophe.commons.model.AdventureStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Progreso de un gato en una aventura.
 */
public record CatAdventure(
        UUID id,
        UUID catId,
        UUID adventureId,
        AdventureStatus status,
        int progressPct,
        Instant startedAt,
        Instant completedAt
) {
    /** Iniciar una nueva aventura. */
    public static CatAdventure start(UUID catId, UUID adventureId) {
        return new CatAdventure(
                UUID.randomUUID(), catId, adventureId,
                AdventureStatus.IN_PROGRESS, 0, Instant.now(), null
        );
    }

    /** Actualizar progreso. */
    public CatAdventure updateProgress(int newPct) {
        if (newPct >= 100) {
            return new CatAdventure(id, catId, adventureId,
                    AdventureStatus.COMPLETED, 100, startedAt, Instant.now());
        }
        return new CatAdventure(id, catId, adventureId, status, newPct, startedAt, completedAt);
    }

    /** Abandonar la aventura. */
    public CatAdventure abandon() {
        return new CatAdventure(id, catId, adventureId,
                AdventureStatus.ABANDONED, progressPct, startedAt, Instant.now());
    }

    public boolean isCompleted() {
        return status == AdventureStatus.COMPLETED;
    }
}
