package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.CatChallenge;
import com.catastrophe.adventures.domain.model.CatChallenge.ChallengeStatus;
import com.catastrophe.adventures.domain.model.Challenge;
import com.catastrophe.adventures.domain.port.in.ChallengeUseCase.ResolveResult;
import com.catastrophe.adventures.domain.port.out.CatChallengeRepository;
import com.catastrophe.adventures.domain.port.out.ChallengeRepository;
import com.catastrophe.adventures.domain.port.out.EventPublisher;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.ChallengeResult;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de ChallengeService — creaci\u00f3n, aceptaci\u00f3n y resoluci\u00f3n
 * de retos PvP, incluyendo el reparto de XP entre ganador, perdedor y empate.
 */
class ChallengeServiceTest {

    private ChallengeRepository challengeRepository;
    private CatChallengeRepository catChallengeRepository;
    private EventPublisher eventPublisher;
    private ChallengeService service;

    private final UUID challengerId = UUID.randomUUID();
    private final UUID opponentId = UUID.randomUUID();
    private final UUID challengeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        challengeRepository = mock(ChallengeRepository.class);
        catChallengeRepository = mock(CatChallengeRepository.class);
        eventPublisher = mock(EventPublisher.class);
        service = new ChallengeService(challengeRepository, catChallengeRepository, eventPublisher);

        when(catChallengeRepository.save(any(CatChallenge.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Challenge challenge(int xpReward) {
        return new Challenge(challengeId, "Carrera de tejados", "Quien llegue antes gana",
                Challenge.TYPE_RACING, xpReward, null, null);
    }

    private CatChallenge active() {
        return new CatChallenge(UUID.randomUUID(), challengerId, challengeId,
                opponentId, ChallengeStatus.ACTIVE, 0, Instant.now());
    }

    @Nested
    @DisplayName("Creaci\u00f3n y aceptaci\u00f3n")
    class CreateAccept {

        @Test
        @DisplayName("create deja el reto en PENDING sin oponente")
        void createsPending() {
            when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge(100)));

            var result = service.create(challengerId, challengeId);

            assertThat(result.status()).isEqualTo(ChallengeStatus.PENDING);
            assertThat(result.opponentId()).isNull();
        }

        @Test
        @DisplayName("create con reto inexistente lanza ResourceNotFoundException")
        void createUnknownThrows() {
            when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(challengerId, challengeId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("accept pasa el reto a ACTIVE con oponente asignado")
        void acceptsPending() {
            var pending = new CatChallenge(UUID.randomUUID(), challengerId, challengeId,
                    null, ChallengeStatus.PENDING, 0, Instant.now());
            when(catChallengeRepository.findById(pending.id())).thenReturn(Optional.of(pending));

            var result = service.accept(pending.id(), opponentId);

            assertThat(result.status()).isEqualTo(ChallengeStatus.ACTIVE);
            assertThat(result.opponentId()).isEqualTo(opponentId);
        }

        @Test
        @DisplayName("Un gato no puede aceptar su propio reto (CHALLENGE_SELF)")
        void cannotAcceptOwn() {
            var pending = new CatChallenge(UUID.randomUUID(), challengerId, challengeId,
                    null, ChallengeStatus.PENDING, 0, Instant.now());
            when(catChallengeRepository.findById(pending.id())).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.accept(pending.id(), challengerId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("CHALLENGE_SELF");
        }

        @Test
        @DisplayName("No se puede aceptar un reto que no est\u00e1 pendiente (CHALLENGE_NOT_PENDING)")
        void cannotAcceptNonPending() {
            var alreadyActive = active();
            when(catChallengeRepository.findById(alreadyActive.id())).thenReturn(Optional.of(alreadyActive));

            assertThatThrownBy(() -> service.accept(alreadyActive.id(), UUID.randomUUID()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("CHALLENGE_NOT_PENDING");
        }

        @Test
        @DisplayName("accept de reto inexistente lanza ResourceNotFoundException")
        void acceptUnknownThrows() {
            var id = UUID.randomUUID();
            when(catChallengeRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.accept(id, opponentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Resoluci\u00f3n y reparto de XP")
    class Resolve {

        @Test
        @DisplayName("El retador gana: WON/LOST y XP completo vs. mitad, dos eventos publicados")
        void challengerWins() {
            var ca = active();
            when(catChallengeRepository.findById(ca.id())).thenReturn(Optional.of(ca));
            when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge(100)));

            ResolveResult result = service.resolve(ca.id(), 80, 30);

            assertThat(result.challenger().status()).isEqualTo(ChallengeStatus.WON);
            assertThat(result.challenger().score()).isEqualTo(80);
            assertThat(result.opponent().status()).isEqualTo(ChallengeStatus.LOST);
            assertThat(result.opponent().score()).isEqualTo(30);
            assertThat(result.opponent().catId()).isEqualTo(opponentId);

            var captor = ArgumentCaptor.forClass(ChallengeCompleted.class);
            verify(eventPublisher, times(2)).publish(captor.capture());
            var events = captor.getAllValues();
            var winner = events.stream().filter(e -> e.result() == ChallengeResult.WON).findFirst().orElseThrow();
            var loser = events.stream().filter(e -> e.result() == ChallengeResult.LOST).findFirst().orElseThrow();
            assertThat(winner.xpEarned()).isEqualTo(100);
            assertThat(loser.xpEarned()).isEqualTo(50);
        }

        @Test
        @DisplayName("El retador pierde: LOST/WON")
        void challengerLoses() {
            var ca = active();
            when(catChallengeRepository.findById(ca.id())).thenReturn(Optional.of(ca));
            when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge(100)));

            ResolveResult result = service.resolve(ca.id(), 10, 90);

            assertThat(result.challenger().status()).isEqualTo(ChallengeStatus.LOST);
            assertThat(result.opponent().status()).isEqualTo(ChallengeStatus.WON);
        }

        @Test
        @DisplayName("Empate: ambos DRAW y XP al 75%")
        void draw() {
            var ca = active();
            when(catChallengeRepository.findById(ca.id())).thenReturn(Optional.of(ca));
            when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge(100)));

            ResolveResult result = service.resolve(ca.id(), 50, 50);

            assertThat(result.challenger().status()).isEqualTo(ChallengeStatus.DRAW);
            assertThat(result.opponent().status()).isEqualTo(ChallengeStatus.DRAW);

            var captor = ArgumentCaptor.forClass(ChallengeCompleted.class);
            verify(eventPublisher, times(2)).publish(captor.capture());
            assertThat(captor.getAllValues()).allMatch(e -> e.xpEarned() == 75);
        }

        @Test
        @DisplayName("No se puede resolver un reto que no est\u00e1 activo (CHALLENGE_NOT_ACTIVE)")
        void cannotResolveNonActive() {
            var pending = new CatChallenge(UUID.randomUUID(), challengerId, challengeId,
                    null, ChallengeStatus.PENDING, 0, Instant.now());
            when(catChallengeRepository.findById(pending.id())).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> service.resolve(pending.id(), 10, 20))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("CHALLENGE_NOT_ACTIVE");

            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("resolve de reto inexistente lanza ResourceNotFoundException")
        void resolveUnknownThrows() {
            var id = UUID.randomUUID();
            when(catChallengeRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(id, 10, 20))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("findPending delega")
        void findPendingDelegates() {
            when(catChallengeRepository.findPendingByChallenge(challengeId)).thenReturn(List.of());
            service.findPending(challengeId);
            verify(catChallengeRepository).findPendingByChallenge(challengeId);
        }

        @Test
        @DisplayName("findByCat delega")
        void findByCatDelegates() {
            when(catChallengeRepository.findByCatId(challengerId)).thenReturn(List.of());
            service.findByCat(challengerId);
            verify(catChallengeRepository).findByCatId(challengerId);
        }
    }
}
