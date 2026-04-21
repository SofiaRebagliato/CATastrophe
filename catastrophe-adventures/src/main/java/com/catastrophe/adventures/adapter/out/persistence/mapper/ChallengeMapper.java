package com.catastrophe.adventures.adapter.out.persistence.mapper;

import com.catastrophe.adventures.adapter.out.persistence.entity.ChallengeEntity;
import com.catastrophe.adventures.domain.model.Challenge;

public final class ChallengeMapper {
    private ChallengeMapper() {}

    public static Challenge toDomain(ChallengeEntity e) {
        return new Challenge(e.getId(), e.getTitle(), e.getDescription(),
                e.getChallengeType(), e.getXpReward(), e.getStartsAt(), e.getEndsAt());
    }
}
