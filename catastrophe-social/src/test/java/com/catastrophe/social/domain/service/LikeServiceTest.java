package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Like;
import com.catastrophe.social.domain.model.Post;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.LikeRepository;
import com.catastrophe.social.domain.port.out.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
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
 * Tests unitarios de LikeService — like/unlike, idempotencia y self-like.
 */
class LikeServiceTest {

    private LikeRepository likeRepository;
    private PostRepository postRepository;
    private EventPublisher eventPublisher;
    private LikeService service;

    private final UUID likerCatId = UUID.randomUUID();
    private final UUID postOwnerCatId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();
    private final Post post = new Post(postId, postOwnerCatId, "x", null, "meow",
            5, 0, Instant.now());

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        postRepository = mock(PostRepository.class);
        eventPublisher = mock(EventPublisher.class);
        service = new LikeService(likeRepository, postRepository, eventPublisher);

        when(likeRepository.save(any(Like.class))).thenAnswer(i -> i.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Like")
    class GivingLike {

        @Test
        @DisplayName("Like en post de otro: persiste, incrementa contador y publica PostLiked")
        void likesPostFromOther() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(false);

            service.like(postId, likerCatId);

            verify(likeRepository).save(any(Like.class));

            // Contador incrementado en el post
            var postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().likeCount()).isEqualTo(6);

            // Evento publicado con postOwnerId correcto
            var eventCaptor = ArgumentCaptor.forClass(PostLiked.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            assertThat(eventCaptor.getValue().catId()).isEqualTo(likerCatId);
            assertThat(eventCaptor.getValue().postOwnerId()).isEqualTo(postOwnerCatId);
        }

        @Test
        @DisplayName("Like duplicado es idempotente (no persiste ni publica)")
        void duplicateIsIdempotent() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(true);

            service.like(postId, likerCatId);

            verify(likeRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Self-like lanza BusinessRuleViolationException con código SELF_LIKE")
        void selfLikeThrows() {
            // Post del propio likerCatId
            var ownPost = new Post(postId, likerCatId, "x", null, "meow", 0, 0, Instant.now());
            when(postRepository.findById(postId)).thenReturn(Optional.of(ownPost));
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(false);

            assertThatThrownBy(() -> service.like(postId, likerCatId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("ti mismo");

            verify(likeRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Like a post inexistente lanza ResourceNotFoundException")
        void postNotFoundThrows() {
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.like(postId, likerCatId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Unlike")
    class TakingLikeBack {

        @Test
        @DisplayName("Unlike borra el like y decrementa el contador del post")
        void unlikesAndDecrements() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(true);

            service.unlike(postId, likerCatId);

            verify(likeRepository).deleteByPostIdAndCatId(postId, likerCatId);

            var postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().likeCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("Unlike sin like previo es idempotente (no borra, no toca contador)")
        void noOpWhenNotLiked() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(false);

            service.unlike(postId, likerCatId);

            verify(likeRepository, never()).deleteByPostIdAndCatId(any(), any());
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Unlike a post inexistente lanza ResourceNotFoundException")
        void postNotFoundThrows() {
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.unlike(postId, likerCatId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Decrementar contador no baja de 0")
        void doesNotGoBelowZero() {
            var emptyPost = new Post(postId, postOwnerCatId, "x", null, "meow",
                    0, 0, Instant.now());
            when(postRepository.findById(postId)).thenReturn(Optional.of(emptyPost));
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(true);

            service.unlike(postId, likerCatId);

            var captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().likeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("hasLiked delega en exists del repo")
        void hasLikedDelegates() {
            when(likeRepository.existsByPostIdAndCatId(postId, likerCatId)).thenReturn(true);

            assertThat(service.hasLiked(postId, likerCatId)).isTrue();
        }

        @Test
        @DisplayName("countByPostId delega")
        void countDelegates() {
            when(likeRepository.countByPostId(postId)).thenReturn(42);

            assertThat(service.countByPostId(postId)).isEqualTo(42);
        }
    }
}
