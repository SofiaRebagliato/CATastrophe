package com.catastrophe.notifications.domain.model;

/**
 * Tipos de notificación que pueden generarse a partir de eventos Kafka.
 * <p>
 * Cada valor mapea 1:N con uno o varios {@code CatastropheEvent}:
 * los eventos sociales (likes, comments, follows) generan notificaciones
 * al propietario del recurso; los de gamificación al propio gato actor.
 */
public enum NotificationType {

    // ── Sociales ──
    POST_LIKED,
    POST_COMMENTED,
    CAT_FOLLOWED,

    // ── Gamificación ──
    ADVENTURE_COMPLETED,
    CHALLENGE_COMPLETED,
    BADGE_EARNED,
    LEVEL_UP
}
