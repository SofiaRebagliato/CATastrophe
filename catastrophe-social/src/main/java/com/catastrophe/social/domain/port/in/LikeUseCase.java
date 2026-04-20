package com.catastrophe.social.domain.port.in;

import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de likes.
 */
public interface LikeUseCase {

    /** Dar like a un post. Idempotente: si ya existe, no hace nada. */
    void like(UUID postId, UUID catId);

    /** Quitar like de un post. */
    void unlike(UUID postId, UUID catId);

    /** Comprobar si un gato ha dado like a un post. */
    boolean hasLiked(UUID postId, UUID catId);

    /** Contar likes de un post. */
    int countByPostId(UUID postId);
}
