package com.catastrophe.profiles.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Un humano (el "asistente" de sus gatos).
 *
 * Record inmutable con métodos factory y de transformación.
 */
public record Human(
        UUID id,
        String username,
        String email,
        String passwordHash,
        String displayName,
        Instant createdAt,
        Instant lastLogin,
        boolean active
) {
    /**
     * Factory method para crear un nuevo humano con valores por defecto.
     */
    public static Human create(String username, String email, String passwordHash, String displayName) {
        return new Human(
                UUID.randomUUID(),
                username,
                email,
                passwordHash,
                displayName,
                Instant.now(),
                null,
                true
        );
    }

    /**
     * Devuelve una copia con el timestamp de último login actualizado.
     */
    public Human withLastLogin(Instant loginTime) {
        return new Human(id, username, email, passwordHash, displayName, createdAt, loginTime, active);
    }

    /**
     * Devuelve una copia con la cuenta desactivada.
     */
    public Human deactivate() {
        return new Human(id, username, email, passwordHash, displayName, createdAt, lastLogin, false);
    }
}
