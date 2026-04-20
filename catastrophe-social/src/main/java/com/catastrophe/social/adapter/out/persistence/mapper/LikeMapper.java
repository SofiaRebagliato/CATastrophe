package com.catastrophe.social.adapter.out.persistence.mapper;

import com.catastrophe.social.adapter.out.persistence.entity.LikeEntity;
import com.catastrophe.social.domain.model.Like;

public final class LikeMapper {

    private LikeMapper() {}

    public static Like toDomain(LikeEntity entity) {
        return new Like(entity.getId(), entity.getPostId(), entity.getCatId(), entity.getCreatedAt());
    }

    public static LikeEntity toEntity(Like like) {
        return new LikeEntity(like.id(), like.postId(), like.catId(), like.createdAt());
    }
}
