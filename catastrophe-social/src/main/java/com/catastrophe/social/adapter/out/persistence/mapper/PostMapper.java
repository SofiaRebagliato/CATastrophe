package com.catastrophe.social.adapter.out.persistence.mapper;

import com.catastrophe.social.adapter.out.persistence.entity.PostEntity;
import com.catastrophe.social.domain.model.Post;

/**
 * Mapper entre el modelo de dominio Post y la entidad JPA PostEntity.
 * Sin dependencias externas — métodos estáticos puros.
 */
public final class PostMapper {

    private PostMapper() {}

    public static Post toDomain(PostEntity entity) {
        return new Post(
                entity.getId(),
                entity.getCatId(),
                entity.getContent(),
                entity.getImageUrl(),
                entity.getPostType(),
                entity.getLikeCount(),
                entity.getCommentCount(),
                entity.getCreatedAt()
        );
    }

    public static PostEntity toEntity(Post post) {
        return new PostEntity(
                post.id(),
                post.catId(),
                post.content(),
                post.imageUrl(),
                post.postType(),
                post.likeCount(),
                post.commentCount(),
                post.createdAt()
        );
    }
}
