package com.catastrophe.social.adapter.out.persistence.mapper;

import com.catastrophe.social.adapter.out.persistence.entity.FollowEntity;
import com.catastrophe.social.domain.model.Follow;

public final class FollowMapper {

    private FollowMapper() {}

    public static Follow toDomain(FollowEntity entity) {
        return new Follow(entity.getId(), entity.getFollowerId(), entity.getFollowedId(), entity.getCreatedAt());
    }

    public static FollowEntity toEntity(Follow follow) {
        return new FollowEntity(follow.id(), follow.followerId(), follow.followedId(), follow.createdAt());
    }
}
