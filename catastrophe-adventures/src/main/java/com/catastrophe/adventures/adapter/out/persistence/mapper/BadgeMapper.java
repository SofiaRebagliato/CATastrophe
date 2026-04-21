package com.catastrophe.adventures.adapter.out.persistence.mapper;

import com.catastrophe.adventures.adapter.out.persistence.entity.BadgeEntity;
import com.catastrophe.adventures.domain.model.Badge;

public final class BadgeMapper {
    private BadgeMapper() {}

    public static Badge toDomain(BadgeEntity e) {
        return new Badge(e.getId(), e.getName(), e.getDescription(), e.getIconUrl(), e.getRarity());
    }
}
