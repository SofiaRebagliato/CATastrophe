package com.catastrophe.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Mensaje privado entre gatos.
 */
public record Message(
        UUID id,
        UUID senderId,
        UUID receiverId,
        String content,
        boolean read,
        Instant createdAt
) {
    public static Message create(UUID senderId, UUID receiverId, String content) {
        return new Message(UUID.randomUUID(), senderId, receiverId, content, false, Instant.now());
    }

    public Message markAsRead() {
        return new Message(id, senderId, receiverId, content, true, createdAt);
    }
}
