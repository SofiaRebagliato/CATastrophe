package com.catastrophe.social.domain.port.out;

import com.catastrophe.social.domain.model.Follow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de seguimientos.
 */
public interface FollowRepository {

    Follow save(Follow follow);

    Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    /** IDs de los gatos que sigue un gato. */
    List<UUID> findFollowedIds(UUID followerId);

    List<Follow> findByFollowerId(UUID followerId);

    List<Follow> findByFollowedId(UUID followedId);

    int countByFollowedId(UUID followedId);

    int countByFollowerId(UUID followerId);
}
