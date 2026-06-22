package com.catastrophe.adventures.adapter.in.messaging;

import com.catastrophe.adventures.domain.port.in.BadgeUseCase;
import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.event.KafkaTopics;
import com.catastrophe.commons.exception.CatastropheExceptions.DuplicateResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumidor Kafka — Concede insignias en respuesta a acciones sociales.
 * <p>
 * Escucha el topic {@code SOCIAL_EVENTS} (donde el módulo social publica
 * {@code MeowPosted}) y otorga la insignia <strong>"Primer Meow"</strong> la
 * primera vez que un gato publica.
 * <p>
 * <strong>¿Por qué un consumer aparte y no el {@code GamificationEventConsumer}?</strong>
 * Aquel solo escucha {@code GAMIFICATION_EVENTS} y se ocupa de los rankings de XP.
 * {@code MeowPosted} es un evento <em>social</em>, que se publica en
 * {@code SOCIAL_EVENTS}; por eso nunca llegaba al consumer de gamificación. Aquí
 * nos suscribimos al topic correcto y damos a las insignias su propio listener
 * (responsabilidad única + offset/grupo independiente).
 * <p>
 * <strong>Idempotencia ("solo la primera vez"):</strong> la garantiza el índice
 * único {@code (cat_id, badge_id)} y el guard de {@link BadgeUseCase#award}: el
 * segundo meow (y siguientes) lanzan {@link DuplicateResourceException}, que aquí
 * ignoramos. Así no hace falta contar publicaciones para saber si es "la primera".
 */
@Component
public class BadgeAwardConsumer {

    private static final Logger log = LoggerFactory.getLogger(BadgeAwardConsumer.class);

    /**
     * UUID fijo de la insignia "Primer Meow", sembrada en
     * {@code V1__create_gamification_tables.sql}.
     */
    private static final UUID FIRST_MEOW_BADGE_ID =
            UUID.fromString("a0000001-0000-0000-0000-000000000001");

    private final BadgeUseCase badgeUseCase;

    public BadgeAwardConsumer(BadgeUseCase badgeUseCase) {
        this.badgeUseCase = badgeUseCase;
    }

    @KafkaListener(topics = KafkaTopics.SOCIAL_EVENTS, groupId = "adventures-badge-awarder")
    public void handleSocialEvent(CatastropheEvent event) {
        // Pattern matching: de momento solo MeowPosted concede insignia.
        if (event instanceof MeowPosted meow) {
            awardFirstMeow(meow.catId());
        }
    }

    private void awardFirstMeow(UUID catId) {
        try {
            badgeUseCase.award(catId, FIRST_MEOW_BADGE_ID);
            log.info("Insignia 'Primer Meow' concedida al gato {}", catId);
        } catch (DuplicateResourceException ex) {
            // Ya la tenía: es su segundo (o enésimo) meow. Comportamiento esperado.
            log.trace("El gato {} ya tenía la insignia 'Primer Meow'", catId);
        } catch (Exception ex) {
            // No relanzamos (mismo criterio que NotificationEventConsumer): preferimos
            // que el offset avance a que el consumer quede atascado por un evento.
            log.error("Error concediendo 'Primer Meow' al gato {}: {}", catId, ex.getMessage(), ex);
        }
    }
}
