package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Post;
import com.catastrophe.social.domain.port.in.PostUseCase.CreatePostCommand;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.FollowRepository;
import com.catastrophe.social.domain.port.out.PostRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de PostService — meows, feed, borrado.
 */
class PostServiceTest {

    private PostRepository postRepository;
    private FollowRepository followRepository;
    private EventPublisher eventPublisher;
    private PostService service;

    private final UUID catId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        followRepository = mock(FollowRepository.class);
        eventPublisher = mock(EventPublisher.class);
        service = new PostService(postRepository, followRepository, eventPublisher);

        // Por defecto, save devuelve el post tal cual entra
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ── Crear post ──

    @Nested
    @DisplayName("Creación de meows")
    class Creation {

        @Test
        @DisplayName("Crear meow guarda el post y publica MeowPosted")
        void createsAndPublishes() {
            var cmd = new CreatePostCommand(catId, "Mi primer meow", null, "meow");

            var result = service.create(cmd);

            assertThat(result.catId()).isEqualTo(catId);
            assertThat(result.content()).isEqualTo("Mi primer meow");
            assertThat(result.likeCount()).isZero();
            assertThat(result.commentCount()).isZero();

            var captor = ArgumentCaptor.forClass(MeowPosted.class);
            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue().catId()).isEqualTo(catId);
            assertThat(captor.getValue().postType()).isEqualTo("meow");
        }

        @Test
        @DisplayName("Contenido vacío lanza BusinessRuleViolationException")
        void rejectsEmptyContent() {
            var cmd = new CreatePostCommand(catId, "   ", null, "meow");

            assertThatThrownBy(() -> service.create(cmd))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("bigotes");

            verify(postRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Contenido null lanza BusinessRuleViolationException")
        void rejectsNullContent() {
            var cmd = new CreatePostCommand(catId, null, null, "meow");

            assertThatThrownBy(() -> service.create(cmd))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("postType null se reemplaza con 'meow' por defecto")
        void defaultsPostTypeToMeow() {
            var cmd = new CreatePostCommand(catId, "contenido", null, null);

            var result = service.create(cmd);

            assertThat(result.postType()).isEqualTo("meow");
        }
    }

    // ── Consultas ──

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("findById delega en el repositorio")
        void findByIdDelegates() {
            var postId = UUID.randomUUID();
            var post = new Post(postId, catId, "x", null, "meow", 0, 0, Instant.now());
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThat(service.findById(postId)).contains(post);
        }

        @Test
        @DisplayName("findByCatId delega con paginación")
        void findByCatIdDelegates() {
            when(postRepository.findByCatId(catId, 0, 20)).thenReturn(List.of());

            service.findByCatId(catId, 0, 20);

            verify(postRepository).findByCatId(catId, 0, 20);
        }
    }

    // ── Feed ──

    @Nested
    @DisplayName("Feed de gatos seguidos")
    class Feed {

        @Test
        @DisplayName("Sin seguidos devuelve lista vacía sin tocar postRepository")
        void emptyFeedWhenNoFollows() {
            when(followRepository.findFollowedIds(catId)).thenReturn(List.of());

            var result = service.getFeed(catId, 0, 20);

            assertThat(result).isEmpty();
            verify(postRepository, never()).findByCatIds(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Con seguidos consulta posts de esos catIds")
        void delegatesToPostRepositoryWhenFollowing() {
            var followed1 = UUID.randomUUID();
            var followed2 = UUID.randomUUID();
            when(followRepository.findFollowedIds(catId))
                    .thenReturn(List.of(followed1, followed2));
            when(postRepository.findByCatIds(List.of(followed1, followed2), 0, 20))
                    .thenReturn(List.of());

            service.getFeed(catId, 0, 20);

            verify(postRepository).findByCatIds(List.of(followed1, followed2), 0, 20);
        }
    }

    // ── Eliminar ──

    @Nested
    @DisplayName("Eliminación")
    class Delete {

        @Test
        @DisplayName("Borrar post propio funciona")
        void deletesOwnPost() {
            var postId = UUID.randomUUID();
            var post = new Post(postId, catId, "x", null, "meow", 0, 0, Instant.now());
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            service.delete(postId, catId);

            verify(postRepository).deleteById(postId);
        }

        @Test
        @DisplayName("Borrar post inexistente lanza ResourceNotFoundException")
        void deleteNonExistentThrows() {
            var postId = UUID.randomUUID();
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(postId, catId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Borrar post ajeno lanza BusinessRuleViolationException con código POST_OWNERSHIP")
        void cannotDeleteOthers() {
            var postId = UUID.randomUUID();
            var otherCatId = UUID.randomUUID();
            var post = new Post(postId, otherCatId, "x", null, "meow", 0, 0, Instant.now());
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> service.delete(postId, catId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no es tuyo");

            verify(postRepository, never()).deleteById(any());
        }
    }
}
