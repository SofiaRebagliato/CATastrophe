package com.catastrophe.social.domain.port.out;

import com.catastrophe.social.domain.model.Comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de comentarios.
 */
public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    List<Comment> findByPostId(UUID postId, int page, int size);

    void deleteById(UUID id);
}
