package com.catastrophe.adventures.adapter.out.persistence.mapper;

import com.catastrophe.adventures.adapter.out.persistence.entity.AdventureEntity;
import com.catastrophe.adventures.domain.model.Adventure;

public final class AdventureMapper {
    private AdventureMapper() {}

    public static Adventure toDomain(AdventureEntity e) {
        return new Adventure(e.getId(), e.getTitle(), e.getDescription(), e.getDifficulty(),
                e.getXpReward(), e.getAdventureType(), e.isRepeatable(),
                e.getAvailableFrom(), e.getAvailableUntil());
    }
}
