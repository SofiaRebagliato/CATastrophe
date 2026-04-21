package com.catastrophe.adventures.adapter.out.persistence.repository;

import com.catastrophe.adventures.adapter.out.persistence.entity.CatChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaCatChallengeRepository extends JpaRepository<CatChallengeEntity, UUID> {

    @Query("SELECT cc FROM CatChallengeEntity cc WHERE cc.challengeId = :challengeId AND cc.status = 'pending'")
    List<CatChallengeEntity> findPendingByChallengeId(@Param("challengeId") UUID challengeId);

    List<CatChallengeEntity> findByCatIdOrderByCreatedAtDesc(UUID catId);
}
