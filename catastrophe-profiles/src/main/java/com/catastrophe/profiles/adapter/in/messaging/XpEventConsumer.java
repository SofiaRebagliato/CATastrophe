package com.catastrophe.profiles.adapter.in.messaging;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.KafkaTopics;
import com.catastrophe.profiles.domain.port.in.CatUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer Kafka — Aplica XP a los gatos cuando llegan eventos de gamificación.
 * <p>
 * Esta es la pieza que <strong>cierra la cadena de XP</strong> de la Fase 4:
 * <ol>
 *   <li>Adventures publica {@code AdventureCompleted} / {@code ChallengeCompleted}
 *       con el {@code xpEarned} ganado en {@code GAMIFICATION_EVENTS}.</li>
 *   <li>Profiles (este consumer) escucha esos eventos y aplica el XP al gato
 *       mediante {@link CatUseCase#applyXpGain}.</li>
 *   <li>El propio service publica un {@code XpGained} con el nuevo total y nivel.</li>
 *   <li>Adventures consume ese {@code XpGained} y actualiza su ranking en Redis.</li>
 *   <li>Notifications detecta el level-up y crea la notificación correspondiente.</li>
 * </ol>
 * <p>
 * Pattern matching exhaustivo sobre el sealed: si en el futuro se añade
 * un evento de gamificación que también deba dar XP, el compilador obliga
 * a tomar una decisión.
 * <p>
 * Idempotencia: la lleva el service mediante {@code processed_xp_events}.
 * Aquí solo desviamos.
 */
@Component
public class XpEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(XpEventConsumer.class);

    private final CatUseCase catUseCase;

    public XpEventConsumer(CatUseCase catUseCase) {
        this.catUseCase = catUseCase;
    }

    @KafkaListener(
            topics = KafkaTopics.GAMIFICATION_EVENTS,
            groupId = "profiles-xp-applier"
    )
    public void onGamificationEvent(CatastropheEvent event) {
        log.debug("XP consumer recibió evento {} (id={})",
                event.getClass().getSimpleName(), event.eventId());

        try {
            switch (event) {
                case AdventureCompleted e ->
                        catUseCase.applyXpGain(e.eventId(), e.catId(), e.xpEarned(), "adventure");

                case ChallengeCompleted e ->
                        catUseCase.applyXpGain(e.eventId(), e.catId(), e.xpEarned(), "challenge");

                // Resto de eventos del topic: los ignoramos explícitamente.
                // Especialmente XpGained: lo emitimos nosotros mismos al aplicar XP,
                // y consumirlo aquí provocaría un bucle.
                default -> log.trace("Evento ignorado por XP consumer: {}",
                        event.getClass().getSimpleName());
            }
        } catch (Exception ex) {
            // No relanzamos: preferimos avanzar el offset y dejar la traza
            // a quedar atascados en un bucle de reintentos.
            log.error("Fallo aplicando XP para evento {} ({}): {}",
                    event.getClass().getSimpleName(), event.eventId(), ex.getMessage(), ex);
        }
    }
}
