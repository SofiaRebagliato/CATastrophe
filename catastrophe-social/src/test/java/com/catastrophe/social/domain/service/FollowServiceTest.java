package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.CatFollowed;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.social.domain.model.Follow;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.FollowRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de FollowService.
 */
class FollowServiceTest {

    private FollowRepository followRepository;
    private EventPublisher eventPublisher;
    private FollowService service;

    private final UUID followerId = UUID.randomUUID();
    private final UUID followedId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        followRepository = mock(FollowRepository.class);
        eventPublisher = mock(EventPublisher.class);
        service = new FollowService(followRepository, eventPublisher);

        when(followRepository.save(any(Follow.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Follow")
    class Following {

        @Test
        @DisplayName("Follow nuevo persiste y publica CatFollowed")
        void newFollow() {
            when(followRepository.existsByFollowerIdAndFollowedId(followerId, followedId))
                    .thenReturn(false);

            var result = service.follow(followerId, followedId);

            assertThat(result.followerId()).isEqualTo(followerId);
            assertThat(result.followedId()).isEqualTo(followedId);

            var captor = ArgumentCaptor.forClass(CatFollowed.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().catId()).isEqualTo(followerId);
            assertThat(captor.getValue().followedCatId()).isEqualTo(followedId);
        }

        @Test
        @DisplayName("Follow duplicado es idempotente: devuelve el existente, no publica")
        void duplicateIsIdempotent() {
            var existing = Follow.create(followerId, followedId);
            when(followRepository.existsByFollowerIdAndFollowedId(followerId, followedId))
                    .thenReturn(true);
            when(followRepository.findByFollowerIdAndFollowedId(followerId, followedId))
                    .thenReturn(Optional.of(existing));

            var result = service.follow(followerId, followedId);

            assertThat(result).isEqualTo(existing);
            verify(followRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Self-follow lanza BusinessRuleViolationException con código SELF_FOLLOW")
        void selfFollowThrows() {
            assertThatThrownBy(() -> service.follow(followerId, followerId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("sí mismo");

            verify(followRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
            // Importante: ni siquiera tocamos exists, la validación es lo primero
            verify(followRepository, never())
                    .existsByFollowerIdAndFollowedId(any(), any());
        }
    }

    @Nested
    @DisplayName("Unfollow")
    class Unfollowing {

        @Test
        @DisplayName("Unfollow delega en deleteByFollowerIdAndFollowedId")
        void delegates() {
            service.unfollow(followerId, followedId);
            verify(followRepository).deleteByFollowerIdAndFollowedId(followerId, followedId);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("isFollowing delega en exists")
        void isFollowingDelegates() {
            when(followRepository.existsByFollowerIdAndFollowedId(followerId, followedId))
                    .thenReturn(true);

            assertThat(service.isFollowing(followerId, followedId)).isTrue();
        }

        @Test
        @DisplayName("getFollowing devuelve los follows del gato como follower")
        void getFollowingDelegates() {
            var follow = Follow.create(followerId, followedId);
            when(followRepository.findByFollowerId(followerId)).thenReturn(List.of(follow));

            assertThat(service.getFollowing(followerId)).hasSize(1);
        }

        @Test
        @DisplayName("getFollowers devuelve los follows en los que el gato es seguido")
        void getFollowersDelegates() {
            var follow = Follow.create(followerId, followedId);
            when(followRepository.findByFollowedId(followedId)).thenReturn(List.of(follow));

            assertThat(service.getFollowers(followedId)).hasSize(1);
        }

        @Test
        @DisplayName("countFollowers y countFollowing delegan correctamente")
        void counters() {
            when(followRepository.countByFollowedId(followedId)).thenReturn(10);
            when(followRepository.countByFollowerId(followerId)).thenReturn(5);

            assertThat(service.countFollowers(followedId)).isEqualTo(10);
            assertThat(service.countFollowing(followerId)).isEqualTo(5);
        }
    }
}
