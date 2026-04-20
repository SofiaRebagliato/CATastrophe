package com.catastrophe.social.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de dominio — Una publicación ("meow") de un gato.
 *
 * Record inmutable: cada modificación crea una nueva instancia.
 * Los contadores like_count y comment_count están desnormalizados
 * para evitar JOINs costosos al renderizar el feed.
 */
public record Post(
        UUID id,
        UUID catId,
        String content,
        String imageUrl,
        String postType,
        int likeCount,
        int commentCount,
        Instant createdAt
) {
    /** Tipos de post válidos. */
    public static final String TYPE_MEOW = "meow";
    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_ADVENTURE_SHARE = "adventure_share";
    public static final String TYPE_CHALLENGE_RESULT = "challenge_result";

    /** Crear un nuevo post con valores por defecto. */
    public static Post create(UUID catId, String content, String imageUrl, String postType) {
        return new Post(
                UUID.randomUUID(),
                catId,
                content,
                imageUrl,
                postType != null ? postType : TYPE_MEOW,
                0,
                0,
                Instant.now()
        );
    }

    /** Incrementar el contador de likes. */
    public Post incrementLikes() {
        return new Post(id, catId, content, imageUrl, postType, likeCount + 1, commentCount, createdAt);
    }

    /** Decrementar el contador de likes. */
    public Post decrementLikes() {
        return new Post(id, catId, content, imageUrl, postType, Math.max(0, likeCount - 1), commentCount, createdAt);
    }

    /** Incrementar el contador de comentarios. */
    public Post incrementComments() {
        return new Post(id, catId, content, imageUrl, postType, likeCount, commentCount + 1, createdAt);
    }
}
