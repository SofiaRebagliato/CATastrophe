package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.Badge;
import com.catastrophe.adventures.domain.model.CatBadge;
import com.catastrophe.adventures.domain.port.out.BadgeRepository;
import com.catastrophe.adventures.domain.port.out.CatBadgeRepository;
import com.catastrophe.adventures.domain.port.out.EventPublisher;
import com.catastrophe.adventures.domain.port.out.RankingCachePort;
import com.catastrophe.commons.event.CatastropheEvent.BadgeEarned;
import com.catastrophe.commons.exception.CatastropheExceptions.DuplicateResourceException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de BadgeService — listado y otorgamiento de insignias.
 */
class BadgeServiceTest {

    private BadgeRepository badgeRepository;
    private CatBadgeRepository catBadgeRepository;
    private EventPublisher eventPublisher;
    private RankingCachePort rankingCache;
    private BadgeService service;

    private final UUID catId = UUID.randomUUID();
    private final UUID badgeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        badgeRepository = mock(BadgeRepository.class);
        catBadgeRepository = mock(CatBadgeRepository.class);
        eventPublisher = mock(EventPublisher.class);
        rankingCache = mock(RankingCachePort.class);
        service = new BadgeService(badgeRepository, catBadgeRepository, eventPublisher, rankingCache);

        when(catBadgeRepository.save(any(CatBadge.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Badge badge() {
        return new Badge(badgeId, "Madrugador", "Despierta antes del amanecer", null, Badge.RARITY_EPIC);
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("findAll delega")
        void findAllDelegates() {
            when(badgeRepository.findAll()).thenReturn(List.of(badge()));
            assertThat(service.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("findById delega")
        void findByIdDelegates() {
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge()));
            assertThat(service.findById(badgeId)).isPresent();
        }

        @Test
        @DisplayName("findByCat delega")
        void findByCatDelegates() {
            when(catBadgeRepository.findByCatId(catId)).thenReturn(List.of());
            service.findByCat(catId);
            verify(catBadgeRepository).findByCatId(catId);
        }
    }

    @Nested
    @DisplayName("Otorgar badge")
    class Award {

        @Test
        @DisplayName("Otorgar guarda, actualiza ranking y publica BadgeEarned")
        void awardsAndPublishes() {
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge()));
            when(catBadgeRepository.exists(catId, badgeId)).thenReturn(false);
            when(catBadgeRepository.countByCatId(catId)).thenReturn(3);

            var result = service.award(catId, badgeId);

            assertThat(result.catId()).isEqualTo(catId);
            assertThat(result.badgeId()).isEqualTo(badgeId);
            verify(rankingCache).updateScore(eq("ranking:badges"), eq(catId), anyDouble());

            var captor = ArgumentCaptor.forClass(BadgeEarned.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().badgeName()).isEqualTo("Madrugador");
            assertThat(captor.getValue().rarity()).isEqualTo(Badge.RARITY_EPIC);
        }

        @Test
        @DisplayName("Badge inexistente lanza ResourceNotFoundException")
        void unknownBadgeThrows() {
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.award(catId, badgeId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(catBadgeRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Badge ya pose\u00eddo lanza DuplicateResourceException")
        void duplicateThrows() {
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge()));
            when(catBadgeRepository.exists(catId, badgeId)).thenReturn(true);

            assertThatThrownBy(() -> service.award(catId, badgeId))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(catBadgeRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }
}
