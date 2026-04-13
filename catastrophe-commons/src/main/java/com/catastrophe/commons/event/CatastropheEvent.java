package com.catastrophe.commons.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Sealed interface que modela TODOS los eventos de dominio de CATastrophe.
 * 
 * Usar sealed + records nos da:
 *  - Exhaustividad garantizada en switch (pattern matching)
 *  - Inmutabilidad por diseño (records)
 *  - Serialización limpia a JSON / Kafka
 * 
 * Cada microservicio produce y/o consume subconjuntos de estos eventos.
 */
public sealed interface CatastropheEvent {

    UUID eventId();
    Instant occurredAt();
    UUID catId();

    // ──────────────────────────────────────────────
    // Eventos de Perfil
    // ──────────────────────────────────────────────

    record CatCreated(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID humanId,
            String name,
            String breed
    ) implements CatastropheEvent {}

    record CatUpdated(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            String field,
            String oldValue,
            String newValue
    ) implements CatastropheEvent {}

    // ──────────────────────────────────────────────
    // Eventos Sociales
    // ──────────────────────────────────────────────

    record MeowPosted(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID postId,
            String postType
    ) implements CatastropheEvent {}

    record PostLiked(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID postId,
            UUID postOwnerId
    ) implements CatastropheEvent {}

    record PostCommented(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID postId,
            UUID postOwnerId,
            UUID commentId
    ) implements CatastropheEvent {}

    record CatFollowed(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID followedCatId
    ) implements CatastropheEvent {}

    // ──────────────────────────────────────────────
    // Eventos de Gamificación
    // ──────────────────────────────────────────────

    record AdventureStarted(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID adventureId,
            String difficulty
    ) implements CatastropheEvent {}

    record AdventureCompleted(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID adventureId,
            int xpEarned
    ) implements CatastropheEvent {}

    record ChallengeCompleted(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID challengeId,
            UUID opponentId,
            ChallengeResult result,
            int score,
            int xpEarned
    ) implements CatastropheEvent {}

    record BadgeEarned(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            UUID badgeId,
            String badgeName,
            String rarity
    ) implements CatastropheEvent {}

    record XpGained(
            UUID eventId,
            Instant occurredAt,
            UUID catId,
            int amount,
            String source,
            int newTotalXp,
            int newLevel
    ) implements CatastropheEvent {}
}
