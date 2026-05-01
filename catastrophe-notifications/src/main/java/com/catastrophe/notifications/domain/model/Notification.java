package com.catastrophe.notifications.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de dominio — Notificación.
 * <p>
 * Inmutable (record). Las transiciones de estado (marcar como leída) devuelven
 * una nueva instancia. Esto encaja con la persistencia JPA porque el adaptador
 * escribe el record completo cada vez.
 */
public record Notification(
        UUID id,
        UUID eventId,
        UUID recipientCatId,
        NotificationType type,
        String message,
        Map<String, Object> payload,
        boolean read,
        Instant createdAt,
        Instant readAt
) {

    public Notification {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(recipientCatId, "recipientCatId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(createdAt, "createdAt");
        if (message.isBlank()) {
            throw new IllegalArgumentException("El mensaje de la notificación no puede estar vacío");
        }
    }

    /**
     * Factory para una notificación recién creada (todavía sin id, sin marcar como leída).
     */
    public static Notification newOne(UUID eventId,
                                      UUID recipientCatId,
                                      NotificationType type,
                                      String message,
                                      Map<String, Object> payload) {
        return new Notification(
                null,
                eventId,
                recipientCatId,
                type,
                message,
                Map.copyOf(payload),
                false,
                Instant.now(),
                null
        );
    }

    /**
     * Devuelve una copia marcada como leída. Si ya estaba leída, devuelve la misma.
     */
    public Notification markAsRead() {
        if (read) {
            return this;
        }
        return new Notification(
                id, eventId, recipientCatId, type, message, payload,
                true, createdAt, Instant.now()
        );
    }
}
