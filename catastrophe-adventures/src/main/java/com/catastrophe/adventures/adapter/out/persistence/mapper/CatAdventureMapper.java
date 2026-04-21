package com.catastrophe.adventures.adapter.out.persistence.mapper;

import com.catastrophe.adventures.adapter.out.persistence.entity.CatAdventureEntity;
import com.catastrophe.adventures.domain.model.CatAdventure;
import com.catastrophe.commons.model.AdventureStatus;

public final class CatAdventureMapper {
    private CatAdventureMapper() {}

    public static CatAdventure toDomain(CatAdventureEntity e) {
        return new CatAdventure(e.getId(), e.getCatId(), e.getAdventureId(),
                AdventureStatus.fromDb(e.getStatus()), e.getProgressPct(),
                e.getStartedAt(), e.getCompletedAt());
    }

    public static CatAdventureEntity toEntity(CatAdventure ca) {
        return new CatAdventureEntity(ca.id(), ca.catId(), ca.adventureId(),
                ca.status().dbValue(), ca.progressPct(), ca.startedAt(), ca.completedAt());
    }
}
