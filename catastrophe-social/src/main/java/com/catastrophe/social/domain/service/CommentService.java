package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.PostCommented;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Comment;
import com.catastrophe.social.domain.port.in.CommentUseCase;
import com.catastrophe.social.domain.port.out.CommentRepository;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de comentarios.
 *
 * Al crear un comentario, se incrementa el contador desnormalizado
 * en el post y se emite un evento Kafka para notificaciones.
 */
@Service
@Transactional
public class CommentService implements CommentUseCase {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final EventPublisher eventPublisher;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          EventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Comment create(CreateCommentCommand command) {
        if (command.content() == null || command.content().isBlank()) {
            throw new BusinessRuleViolationException(
                    "COMMENT_CONTENT_REQUIRED",
                    "Un comentario vacío no tiene sentido. ¡Escribe algo, gatito!"
            );
        }

        var post = postRepository.findById(command.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", command.postId()));

        var comment = Comment.create(command.postId(), command.catId(), command.content());
        var saved = commentRepository.save(comment);

        // Incrementar contador desnormalizado en el post
        postRepository.save(post.incrementComments());

        // Emitir evento (notifica al dueño del post)
        eventPublisher.publish(new PostCommented(
                UUID.randomUUID(),
                Instant.now(),
                command.catId(),
                command.postId(),
                post.catId(), // postOwnerId
                saved.id()
        ));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findByPostId(UUID postId, int page, int size) {
        return commentRepository.findByPostId(postId, page, size);
    }

    @Override
    public void delete(UUID commentId, UUID catId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.catId().equals(catId)) {
            throw new BusinessRuleViolationException(
                    "COMMENT_OWNERSHIP",
                    "¡Ese comentario no es tuyo!"
            );
        }

        commentRepository.deleteById(commentId);
    }
}
