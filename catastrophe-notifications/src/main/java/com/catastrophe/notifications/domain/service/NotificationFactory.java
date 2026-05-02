package com.catastrophe.notifications.domain.service;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.*;
import com.catastrophe.notifications.domain.model.Notification;
import com.catastrophe.notifications.domain.model.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Fábrica de notificaciones a partir de eventos del bus.
 * <p>
 * Centraliza la decisión de <em>qué</em> evento genera notificación y <em>para quién</em>.
 * Aprovecha el pattern matching exhaustivo sobre el {@code sealed CatastropheEvent}
 * para que cualquier evento nuevo añadido al sealed obligue a tomar una decisión
 * explícita (notificar o ignorar) en compilación.
 * <p>
 * Devuelve {@link Optional#empty()} para eventos que no deben producir notificación
 * (ej: {@code CatCreated}, {@code MeowPosted}, {@code AdventureStarted}, o un
 * {@code XpGained} que no es level-up).
 */
@Component
public class NotificationFactory {

    /**
     * Convierte un evento en una notificación lista para persistir, o
     * {@link Optional#empty()} si el evento no debe notificarse.
     */
    public Optional<Notification> from(CatastropheEvent event) {
        // Pattern matching exhaustivo. El compilador exige cubrir todos los casos
        // del sealed CatastropheEvent.
        return switch (event) {

            // ── Sociales: notifican al dueño del recurso ──
            case PostLiked e -> Optional.of(socialNotification(
                    e,
                    e.postOwnerId(),
                    NotificationType.POST_LIKED,
                    "A un gato le ha gustado tu meow 😻",
                    Map.of(
                            "postId", e.postId().toString(),
                            "likerCatId", e.catId().toString()
                    )
            ));

            case PostCommented e -> Optional.of(socialNotification(
                    e,
                    e.postOwnerId(),
                    NotificationType.POST_COMMENTED,
                    "Tienes un nuevo comentario en tu meow 💬",
                    Map.of(
                            "postId", e.postId().toString(),
                            "commentId", e.commentId().toString(),
                            "commenterCatId", e.catId().toString()
                    )
            ));

            case CatFollowed e -> Optional.of(socialNotification(
                    e,
                    e.followedCatId(),
                    NotificationType.CAT_FOLLOWED,
                    "¡Tienes un nuevo seguidor felino! 🐾",
                    Map.of(
                            "followerCatId", e.catId().toString()
                    )
            ));

            // ── Gamificación: notifican al propio gato actor ──
            case AdventureCompleted e -> Optional.of(notificationFor(
                    e,
                    e.catId(),
                    NotificationType.ADVENTURE_COMPLETED,
                    "¡Aventura completada! Has ganado %d XP 🏆".formatted(e.xpEarned()),
                    Map.of(
                            "adventureId", e.adventureId().toString(),
                            "xpEarned", e.xpEarned()
                    )
            ));

            case ChallengeCompleted e -> Optional.of(notificationFor(
                    e,
                    e.catId(),
                    NotificationType.CHALLENGE_COMPLETED,
                    challengeMessage(e),
                    Map.of(
                            "challengeId", e.challengeId().toString(),
                            "opponentId", e.opponentId().toString(),
                            "result", e.result().name(),
                            "score", e.score(),
                            "xpEarned", e.xpEarned()
                    )
            ));

            case BadgeEarned e -> Optional.of(notificationFor(
                    e,
                    e.catId(),
                    NotificationType.BADGE_EARNED,
                    "¡Has desbloqueado la insignia '%s' (%s)! 🎖️".formatted(e.badgeName(), e.rarity()),
                    Map.of(
                            "badgeId", e.badgeId().toString(),
                            "badgeName", e.badgeName(),
                            "rarity", e.rarity()
                    )
            ));

            // XpGained se procesa fuera de esta fábrica: requiere comparar contra
            // el último nivel conocido del gato (estado externo). El service lo
            // gestiona invocando explícitamente fromLevelUp(...) cuando procede.
            case XpGained _ -> Optional.empty();

            // ── Eventos que se ignoran de forma explícita ──
            // El compilador nos obliga a listarlos: si el sealed añade nuevos eventos,
            // este switch deja de compilar hasta que tomemos una decisión.
            case CatCreated _,
                 CatUpdated _,
                 MeowPosted _,
                 AdventureStarted _ -> Optional.empty();
        };
    }

    /**
     * Construye explícitamente la notificación de level-up. Se invoca solo cuando
     * el service ha confirmado, contra el tracker, que efectivamente hubo subida.
     */
    public Notification fromLevelUp(XpGained event) {
        return Notification.newOne(
                event.eventId(),
                event.catId(),
                NotificationType.LEVEL_UP,
                "¡Subiste al nivel %d! ✨".formatted(event.newLevel()),
                Map.of(
                        "newLevel", event.newLevel(),
                        "newTotalXp", event.newTotalXp(),
                        "source", event.source()
                )
        );
    }

    // ── Helpers ──

    private Notification notificationFor(CatastropheEvent event,
                                         java.util.UUID recipientCatId,
                                         NotificationType type,
                                         String message,
                                         Map<String, Object> payload) {
        return Notification.newOne(event.eventId(), recipientCatId, type, message, payload);
    }

    /**
     * Variante sintáctica para eventos sociales — idéntica semánticamente a
     * {@link #notificationFor}, separada por claridad de lectura en el switch.
     */
    private Notification socialNotification(CatastropheEvent event,
                                            java.util.UUID recipientCatId,
                                            NotificationType type,
                                            String message,
                                            Map<String, Object> payload) {
        return notificationFor(event, recipientCatId, type, message, payload);
    }

    private String challengeMessage(ChallengeCompleted e) {
        return switch (e.result()) {
            case WON  -> "¡Has ganado el reto! +%d XP 🥇".formatted(e.xpEarned());
            case LOST -> "Has perdido el reto. +%d XP de consolación 😿".formatted(e.xpEarned());
            case DRAW -> "El reto ha terminado en empate. +%d XP 🤝".formatted(e.xpEarned());
        };
    }
}
