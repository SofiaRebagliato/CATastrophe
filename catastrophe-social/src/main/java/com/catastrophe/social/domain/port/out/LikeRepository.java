package com.catastrophe.social.domain.port.out;

import com.catastrophe.social.domain.model.Like;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de likes.
 */
public interface LikeRepository {

    Like save(Like like);

    Optional<Like> findByPostIdAndCatId(UUID postId, UUID catId);

    boolean existsByPostIdAndCatId(UUID postId, UUID catId);

    void deleteByPostIdAndCatId(UUID postId, UUID catId);

    int countByPostId(UUID postId);
}
