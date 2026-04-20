package com.catastrophe.social.domain.port.in;

import com.catastrophe.social.domain.model.Follow;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de seguimientos entre gatos.
 */
public interface FollowUseCase {

    /** Seguir a otro gato. */
    Follow follow(UUID followerId, UUID followedId);

    /** Dejar de seguir a un gato. */
    void unfollow(UUID followerId, UUID followedId);

    /** Comprobar si un gato sigue a otro. */
    boolean isFollowing(UUID followerId, UUID followedId);

    /** Obtener la lista de gatos que sigue un gato. */
    List<Follow> getFollowing(UUID catId);

    /** Obtener la lista de seguidores de un gato. */
    List<Follow> getFollowers(UUID catId);

    /** Contar seguidores. */
    int countFollowers(UUID catId);

    /** Contar seguidos. */
    int countFollowing(UUID catId);
}
