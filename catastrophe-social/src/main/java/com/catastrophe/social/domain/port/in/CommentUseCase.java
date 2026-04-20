package com.catastrophe.social.domain.port.in;

import com.catastrophe.social.domain.model.Comment;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de comentarios.
 */
public interface CommentUseCase {

    /** Comentar en un post. */
    Comment create(CreateCommentCommand command);

    /** Obtener comentarios de un post, paginados. */
    List<Comment> findByPostId(UUID postId, int page, int size);

    /** Eliminar un comentario (solo el autor). */
    void delete(UUID commentId, UUID catId);

    record CreateCommentCommand(
            UUID postId,
            UUID catId,
            String content
    ) {}
}
