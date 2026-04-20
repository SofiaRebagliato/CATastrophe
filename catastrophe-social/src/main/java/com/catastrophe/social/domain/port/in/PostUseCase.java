package com.catastrophe.social.domain.port.in;

import com.catastrophe.social.domain.model.Post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de publicaciones (meows).
 */
public interface PostUseCase {

    /** Crear un nuevo meow. */
    Post create(CreatePostCommand command);

    /** Buscar post por id. */
    Optional<Post> findById(UUID id);

    /** Obtener posts de un gato específico, ordenados por fecha desc. */
    List<Post> findByCatId(UUID catId, int page, int size);

    /** Feed: posts de los gatos seguidos por catId, paginado. */
    List<Post> getFeed(UUID catId, int page, int size);

    /** Eliminar un post (solo el dueño). */
    void delete(UUID postId, UUID catId);

    // ── Commands ──

    record CreatePostCommand(
            UUID catId,
            String content,
            String imageUrl,
            String postType
    ) {}
}
