package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.PostCommented;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Comment;
import com.catastrophe.social.domain.model.Post;
import com.catastrophe.social.domain.port.in.CommentUseCase.CreateCommentCommand;
import com.catastrophe.social.domain.port.out.CommentRepository;
import com.catastrophe.social.domain.port.out.EventPublisher;
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
 * Tests unitarios de CommentService.
 */
class CommentServiceTest {

    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private EventPublisher eventPublisher;
    private CommentService service;

    private final UUID commenterCatId = UUID.randomUUID();
    private final UUID postOwnerCatId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        eventPublisher = mock(EventPublisher.class);
        service = new CommentService(commentRepository, postRepository, eventPublisher);

        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Creación de comentarios")
    class Creation {

        private final Post post = new Post(postId, postOwnerCatId, "contenido", null, "meow",
                0, 0, Instant.now());

        @Test
        @DisplayName("Crear comentario guarda, incrementa contador del post y publica PostCommented")
        void createsAndPublishes() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            var cmd = new CreateCommentCommand(postId, commenterCatId, "Buen meow");

            var result = service.create(cmd);

            assertThat(result.catId()).isEqualTo(commenterCatId);
            assertThat(result.postId()).isEqualTo(postId);
            assertThat(result.content()).isEqualTo("Buen meow");

            // Verificar incremento de contador desnormalizado
            var postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().commentCount()).isEqualTo(1);

            // Verificar evento — el postOwnerId es el del post, no el del commenter
            var eventCaptor = ArgumentCaptor.forClass(PostCommented.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            assertThat(eventCaptor.getValue().catId()).isEqualTo(commenterCatId);
            assertThat(eventCaptor.getValue().postOwnerId()).isEqualTo(postOwnerCatId);
        }

        @Test
        @DisplayName("Comentar en post inexistente lanza ResourceNotFoundException")
        void postNotFoundThrows() {
            when(postRepository.findById(postId)).thenReturn(Optional.empty());
            var cmd = new CreateCommentCommand(postId, commenterCatId, "hola");

            assertThatThrownBy(() -> service.create(cmd))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(commentRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("Contenido vacío lanza BusinessRuleViolationException")
        void rejectsEmptyContent() {
            var cmd = new CreateCommentCommand(postId, commenterCatId, "  ");

            assertThatThrownBy(() -> service.create(cmd))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("vacío");

            // Ni siquiera tocamos el postRepository
            verify(postRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("findByPostId delega con paginación")
        void delegates() {
            service.findByPostId(postId, 0, 20);
            verify(commentRepository).findByPostId(postId, 0, 20);
        }
    }

    @Nested
    @DisplayName("Eliminación")
    class Delete {

        @Test
        @DisplayName("Borrar comentario propio funciona")
        void deletesOwn() {
            var commentId = UUID.randomUUID();
            var comment = new Comment(commentId, postId, commenterCatId, "x", Instant.now());
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            service.delete(commentId, commenterCatId);

            verify(commentRepository).deleteById(commentId);
        }

        @Test
        @DisplayName("Borrar comentario ajeno lanza BusinessRuleViolationException")
        void cannotDeleteOthers() {
            var commentId = UUID.randomUUID();
            var otherCatId = UUID.randomUUID();
            var comment = new Comment(commentId, postId, otherCatId, "x", Instant.now());
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> service.delete(commentId, commenterCatId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no es tuyo");

            verify(commentRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Borrar comentario inexistente lanza ResourceNotFoundException")
        void deleteNonExistentThrows() {
            var commentId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(commentId, commenterCatId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
