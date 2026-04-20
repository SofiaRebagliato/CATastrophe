package com.catastrophe.social.domain.port.out;

import com.catastrophe.social.domain.model.Post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de posts.
 */
public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(UUID id);

    List<Post> findByCatId(UUID catId, int page, int size);

    /** Feed: posts de una lista de catIds, ordenados por fecha desc. */
    List<Post> findByCatIds(List<UUID> catIds, int page, int size);

    void deleteById(UUID id);
}
