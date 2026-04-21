package com.catastrophe.adventures.adapter.out.persistence.mapper;

import com.catastrophe.adventures.adapter.out.persistence.entity.CatBadgeEntity;
import com.catastrophe.adventures.domain.model.CatBadge;

public final class CatBadgeMapper {
    private CatBadgeMapper() {}

    public static CatBadge toDomain(CatBadgeEntity e) {
        return new CatBadge(e.getId(), e.getCatId(), e.getBadgeId(), e.getEarnedAt());
    }

    public static CatBadgeEntity toEntity(CatBadge cb) {
        return new CatBadgeEntity(cb.id(), cb.catId(), cb.badgeId(), cb.earnedAt());
    }
}
