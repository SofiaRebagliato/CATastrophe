package com.catastrophe.profiles.adapter.in.messaging;

import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.event.ChallengeResult;
import com.catastrophe.profiles.domain.port.in.CatUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests del consumer de XP. Verifican que el switch enruta correctamente
 * cada evento de gamificación al use case con la fuente apropiada.
 */
class XpEventConsumerTest {

    private CatUseCase useCase;
    private XpEventConsumer consumer;

    @BeforeEach
    void setUp() {
        useCase = mock(CatUseCase.class);
        consumer = new XpEventConsumer(useCase);
    }

    @Test
    @DisplayName("AdventureCompleted aplica XP con source='adventure'")
    void adventureCompleted() {
        var eventId = UUID.randomUUID();
        var catId = UUID.randomUUID();
        var event = new AdventureCompleted(eventId, Instant.now(),
                catId, UUID.randomUUID(), 75);

        consumer.onGamificationEvent(event);

        verify(useCase).applyXpGain(eventId, catId, 75, "adventure");
    }

    @Test
    @DisplayName("ChallengeCompleted aplica XP con source='challenge'")
    void challengeCompleted() {
        var eventId = UUID.randomUUID();
        var catId = UUID.randomUUID();
        var event = new ChallengeCompleted(eventId, Instant.now(),
                catId, UUID.randomUUID(), UUID.randomUUID(),
                ChallengeResult.WON, 100, 50);

        consumer.onGamificationEvent(event);

        verify(useCase).applyXpGain(eventId, catId, 50, "challenge");
    }

    @Test
    @DisplayName("XpGained se ignora (lo emitimos nosotros mismos: evita bucle)")
    void xpGainedIsIgnored() {
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), 50, "adventure", 50, 1);

        consumer.onGamificationEvent(event);

        verify(useCase, never()).applyXpGain(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("AdventureStarted se ignora")
    void adventureStartedIsIgnored() {
        var event = new AdventureStarted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "easy");

        consumer.onGamificationEvent(event);

        verify(useCase, never()).applyXpGain(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("Excepción del use case no rompe el consumer (offset avanza)")
    void exceptionIsSwallowed() {
        var event = new AdventureCompleted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), 50);
        doThrow(new RuntimeException("BD caída"))
                .when(useCase).applyXpGain(any(), any(), eq(50), eq("adventure"));

        // No debe relanzar
        consumer.onGamificationEvent(event);
    }
}
