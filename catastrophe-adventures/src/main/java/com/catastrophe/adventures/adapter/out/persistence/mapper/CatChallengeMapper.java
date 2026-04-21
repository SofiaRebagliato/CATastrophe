package com.catastrophe.adventures.adapter.out.persistence.mapper;

import com.catastrophe.adventures.adapter.out.persistence.entity.CatChallengeEntity;
import com.catastrophe.adventures.domain.model.CatChallenge;
import com.catastrophe.adventures.domain.model.CatChallenge.ChallengeStatus;

public final class CatChallengeMapper {
    private CatChallengeMapper() {}

    public static CatChallenge toDomain(CatChallengeEntity e) {
        return new CatChallenge(e.getId(), e.getCatId(), e.getChallengeId(),
                e.getOpponentId(), ChallengeStatus.fromDb(e.getStatus()),
                e.getScore(), e.getCreatedAt());
    }

    public static CatChallengeEntity toEntity(CatChallenge cc) {
        return new CatChallengeEntity(cc.id(), cc.catId(), cc.challengeId(),
                cc.opponentId(), cc.status().dbValue(), cc.score(), cc.createdAt());
    }
}
