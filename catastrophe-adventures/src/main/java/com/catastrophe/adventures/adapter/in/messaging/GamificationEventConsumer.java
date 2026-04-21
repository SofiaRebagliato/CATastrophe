package com.catastrophe.adventures.adapter.in.messaging;

import com.catastrophe.adventures.domain.port.in.RankingUseCase;
import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.*;
import com.catastrophe.commons.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka — Procesa eventos de gamificación para actualizar rankings.
 *
 * Usa pattern matching exhaustivo sobre el sealed interface CatastropheEvent
 * para procesar cada tipo de evento de forma segura en compilación.
 */
@Component
public class GamificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(GamificationEventConsumer.class);

    private final RankingUseCase rankingUseCase;

    public GamificationEventConsumer(RankingUseCase rankingUseCase) {
        this.rankingUseCase = rankingUseCase;
    }

    @KafkaListener(topics = KafkaTopics.GAMIFICATION_EVENTS, groupId = "adventures-ranking-updater")
    public void handleGamificationEvent(CatastropheEvent event) {
        log.debug("Recibido evento de gamificación: {}", event.getClass().getSimpleName());

        // Pattern matching exhaustivo con Java 21
        switch (event) {
            case AdventureCompleted e -> {
                log.info("Aventura completada por gato {}: +{} XP", e.catId(), e.xpEarned());
            }
            case ChallengeCompleted e -> {
                log.info("Reto completado por gato {}: resultado={}, +{} XP",
                        e.catId(), e.result(), e.xpEarned());
            }
            case XpGained e -> {
                log.info("XP actualizado para gato {}: total={}, nivel={}",
                        e.catId(), e.newTotalXp(), e.newLevel());
                rankingUseCase.updateScore(e.catId(), e.newTotalXp());
            }
            case BadgeEarned e -> {
                log.info("Badge '{}' ({}) obtenido por gato {}",
                        e.badgeName(), e.rarity(), e.catId());
            }
            case AdventureStarted e ->
                log.debug("Aventura iniciada (ignorado por ranking): {}", e.adventureId());
            // Eventos de otros dominios — no relevantes para este consumer
            case CatCreated _, CatUpdated _, MeowPosted _, PostLiked _,
                 PostCommented _, CatFollowed _ ->
                log.trace("Evento no relevante para rankings: {}", event.getClass().getSimpleName());
        }
    }
}
