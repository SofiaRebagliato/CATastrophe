package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.Adventure;
import com.catastrophe.adventures.domain.model.Badge;
import com.catastrophe.adventures.domain.model.CatAdventure;
import com.catastrophe.adventures.domain.port.in.AdventureUseCase.StartAdventureCommand;
import com.catastrophe.adventures.domain.port.out.AdventureRepository;
import com.catastrophe.adventures.domain.port.out.BadgeRepository;
import com.catastrophe.adventures.domain.port.out.CatAdventureRepository;
import com.catastrophe.adventures.domain.port.out.CatBadgeRepository;
import com.catastrophe.adventures.domain.port.out.EventPublisher;
import com.catastrophe.adventures.domain.port.out.RankingCachePort;
import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.commons.model.AdventureStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de AdventureService — inicio, progreso, completación,
 * abandono y otorgamiento de badges al completar.
 */
class AdventureServiceTest {

    private AdventureRepository adventureRepository;
    private CatAdventureRepository catAdventureRepository;
    private BadgeRepository badgeRepository;
    private CatBadgeRepository catBadgeRepository;
    private EventPublisher eventPublisher;
    private RankingCachePort rankingCache;
    private AdventureService service;

    private final UUID catId = UUID.randomUUID();
    private final UUID adventureId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adventureRepository = mock(AdventureRepository.class);
        catAdventureRepository = mock(CatAdventureRepository.class);
        badgeRepository = mock(BadgeRepository.class);
        catBadgeRepository = mock(CatBadgeRepository.class);
        eventPublisher = mock(EventPublisher.class);
        rankingCache = mock(RankingCachePort.class);
        service = new AdventureService(adventureRepository, catAdventureRepository,
                badgeRepository, catBadgeRepository, eventPublisher, rankingCache);

        // save devuelve el catAdventure tal cual entra
        when(catAdventureRepository.save(any(CatAdventure.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Adventure availableAdventure(int xpReward) {
        return new Adventure(adventureId, "Cazar el l\u00e1ser", "Atrapa el punto rojo",
                Adventure.DIFFICULTY_EASY, xpReward, Adventure.TYPE_DAILY, false,
                Instant.now().minus(1, ChronoUnit.DAYS), null);
    }

    // ── Listado y consultas ──

    @Nested
    @DisplayName("Listado de aventuras disponibles")
    class Listing {

        @Test
        @DisplayName("type null devuelve todas")
        void nullTypeReturnsAll() {
            when(adventureRepository.findAll()).thenReturn(List.of(availableAdventure(50)));

            service.findAvailable(null);

            verify(adventureRepository).findAll();
            verify(adventureRepository, never()).findByType(anyString());
        }

        @Test
        @DisplayName("type en blanco devuelve todas")
        void blankTypeReturnsAll() {
            when(adventureRepository.findAll()).thenReturn(List.of());

            service.findAvailable("   ");

            verify(adventureRepository).findAll();
        }

        @Test
        @DisplayName("type concreto filtra por tipo")
        void concreteTypeFilters() {
            when(adventureRepository.findByType(Adventure.TYPE_DAILY)).thenReturn(List.of());

            service.findAvailable(Adventure.TYPE_DAILY);

            verify(adventureRepository).findByType(Adventure.TYPE_DAILY);
            verify(adventureRepository, never()).findAll();
        }

        @Test
        @DisplayName("findById delega en el repositorio")
        void findByIdDelegates() {
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(availableAdventure(50)));

            assertThat(service.findById(adventureId)).isPresent();
        }
    }

    // ── Iniciar aventura ──

    @Nested
    @DisplayName("Iniciar aventura")
    class Start {

        @Test
        @DisplayName("Iniciar guarda en progreso y publica AdventureStarted")
        void startsAndPublishes() {
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(availableAdventure(50)));
            when(catAdventureRepository.existsActive(catId, adventureId)).thenReturn(false);

            var result = service.start(new StartAdventureCommand(catId, adventureId));

            assertThat(result.status()).isEqualTo(AdventureStatus.IN_PROGRESS);
            assertThat(result.progressPct()).isZero();

            var captor = ArgumentCaptor.forClass(AdventureStarted.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().catId()).isEqualTo(catId);
            assertThat(captor.getValue().adventureId()).isEqualTo(adventureId);
            assertThat(captor.getValue().difficulty()).isEqualTo(Adventure.DIFFICULTY_EASY);
        }

        @Test
        @DisplayName("Aventura inexistente lanza ResourceNotFoundException")
        void unknownAdventureThrows() {
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.start(new StartAdventureCommand(catId, adventureId)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(catAdventureRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Aventura fuera de su ventana lanza BusinessRuleViolationException (ADVENTURE_NOT_AVAILABLE)")
        void notAvailableThrows() {
            var future = new Adventure(adventureId, "t", "d", Adventure.DIFFICULTY_EASY, 50,
                    Adventure.TYPE_DAILY, false, Instant.now().plus(1, ChronoUnit.DAYS), null);
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(future));

            assertThatThrownBy(() -> service.start(new StartAdventureCommand(catId, adventureId)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("ADVENTURE_NOT_AVAILABLE");
        }

        @Test
        @DisplayName("Aventura ya activa lanza BusinessRuleViolationException (ADVENTURE_ALREADY_ACTIVE)")
        void alreadyActiveThrows() {
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(availableAdventure(50)));
            when(catAdventureRepository.existsActive(catId, adventureId)).thenReturn(true);

            assertThatThrownBy(() -> service.start(new StartAdventureCommand(catId, adventureId)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("ADVENTURE_ALREADY_ACTIVE");

            verify(catAdventureRepository, never()).save(any());
        }
    }

    // ── Progreso y completación ──

    @Nested
    @DisplayName("Progreso y completaci\u00f3n")
    class Progress {

        private CatAdventure inProgress() {
            return new CatAdventure(UUID.randomUUID(), catId, adventureId,
                    AdventureStatus.IN_PROGRESS, 0, Instant.now(), null);
        }

        @Test
        @DisplayName("Progreso por debajo de 100 no dispara completaci\u00f3n")
        void partialProgressDoesNotComplete() {
            var ca = inProgress();
            when(catAdventureRepository.findById(ca.id())).thenReturn(Optional.of(ca));

            var result = service.updateProgress(ca.id(), catId, 40);

            assertThat(result.status()).isEqualTo(AdventureStatus.IN_PROGRESS);
            assertThat(result.progressPct()).isEqualTo(40);
            verify(eventPublisher, never()).publish(any(AdventureCompleted.class));
        }

        @Test
        @DisplayName("Llegar al 100% completa, otorga badge, actualiza ranking y publica AdventureCompleted")
        void reaching100Completes() {
            var ca = inProgress();
            var badgeId = UUID.randomUUID();
            when(catAdventureRepository.findById(ca.id())).thenReturn(Optional.of(ca));
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(availableAdventure(50)));
            when(badgeRepository.findBadgeIdsByAdventure(adventureId)).thenReturn(List.of(badgeId));
            when(catBadgeRepository.exists(catId, badgeId)).thenReturn(false);
            when(catBadgeRepository.countByCatId(catId)).thenReturn(1);

            var result = service.updateProgress(ca.id(), catId, 100);

            assertThat(result.status()).isEqualTo(AdventureStatus.COMPLETED);
            verify(catBadgeRepository).save(any());
            verify(rankingCache).updateScore(eq("ranking:badges"), eq(catId), anyDouble());

            var captor = ArgumentCaptor.forClass(AdventureCompleted.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().xpEarned()).isEqualTo(50);
        }

        @Test
        @DisplayName("Un badge ya pose\u00eddo no se vuelve a otorgar")
        void doesNotReAwardOwnedBadge() {
            var ca = inProgress();
            var badgeId = UUID.randomUUID();
            when(catAdventureRepository.findById(ca.id())).thenReturn(Optional.of(ca));
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(availableAdventure(50)));
            when(badgeRepository.findBadgeIdsByAdventure(adventureId)).thenReturn(List.of(badgeId));
            when(catBadgeRepository.exists(catId, badgeId)).thenReturn(true);

            service.complete(ca.id(), catId);

            verify(catBadgeRepository, never()).save(any());
        }

        @Test
        @DisplayName("complete fuerza el progreso al 100%")
        void completeForces100() {
            var ca = inProgress();
            when(catAdventureRepository.findById(ca.id())).thenReturn(Optional.of(ca));
            when(adventureRepository.findById(adventureId)).thenReturn(Optional.of(availableAdventure(50)));
            when(badgeRepository.findBadgeIdsByAdventure(adventureId)).thenReturn(List.of());

            var result = service.complete(ca.id(), catId);

            assertThat(result.progressPct()).isEqualTo(100);
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("Aventura inexistente lanza ResourceNotFoundException")
        void unknownCatAdventureThrows() {
            var id = UUID.randomUUID();
            when(catAdventureRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProgress(id, catId, 50))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Aventura de otro gato lanza BusinessRuleViolationException (ADVENTURE_OWNERSHIP)")
        void otherCatThrows() {
            var ca = inProgress();
            when(catAdventureRepository.findById(ca.id())).thenReturn(Optional.of(ca));

            assertThatThrownBy(() -> service.updateProgress(ca.id(), UUID.randomUUID(), 50))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("ADVENTURE_OWNERSHIP");
        }

        @Test
        @DisplayName("Aventura ya completada lanza BusinessRuleViolationException (ADVENTURE_ALREADY_COMPLETED)")
        void alreadyCompletedThrows() {
            var done = new CatAdventure(UUID.randomUUID(), catId, adventureId,
                    AdventureStatus.COMPLETED, 100, Instant.now(), Instant.now());
            when(catAdventureRepository.findById(done.id())).thenReturn(Optional.of(done));

            assertThatThrownBy(() -> service.updateProgress(done.id(), catId, 50))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("rule").isEqualTo("ADVENTURE_ALREADY_COMPLETED");
        }
    }

    // ── Abandonar ──

    @Nested
    @DisplayName("Abandonar aventura")
    class Abandon {

        @Test
        @DisplayName("Abandonar marca la aventura como ABANDONED")
        void abandons() {
            var ca = new CatAdventure(UUID.randomUUID(), catId, adventureId,
                    AdventureStatus.IN_PROGRESS, 30, Instant.now(), null);
            when(catAdventureRepository.findById(ca.id())).thenReturn(Optional.of(ca));

            var result = service.abandon(ca.id(), catId);

            assertThat(result.status()).isEqualTo(AdventureStatus.ABANDONED);
            verify(eventPublisher, never()).publish(any());
        }
    }

    // ── Delegaciones de consulta ──

    @Nested
    @DisplayName("Consultas de gato")
    class CatQueries {

        @Test
        @DisplayName("findActiveByCat delega")
        void activeDelegates() {
            when(catAdventureRepository.findActiveByCatId(catId)).thenReturn(List.of());
            service.findActiveByCat(catId);
            verify(catAdventureRepository).findActiveByCatId(catId);
        }

        @Test
        @DisplayName("findHistoryByCat delega con paginaci\u00f3n")
        void historyDelegates() {
            when(catAdventureRepository.findByCatId(catId, 0, 20)).thenReturn(List.of());
            service.findHistoryByCat(catId, 0, 20);
            verify(catAdventureRepository).findByCatId(catId, 0, 20);
        }
    }
}
