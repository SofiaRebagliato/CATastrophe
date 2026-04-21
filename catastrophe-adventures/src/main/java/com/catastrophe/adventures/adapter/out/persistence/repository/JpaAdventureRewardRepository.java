package com.catastrophe.adventures.adapter.out.persistence.repository;

import com.catastrophe.adventures.adapter.out.persistence.entity.AdventureRewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaAdventureRewardRepository extends JpaRepository<AdventureRewardEntity, UUID> {

    @Query("SELECT ar.badgeId FROM AdventureRewardEntity ar WHERE ar.adventureId = :adventureId")
    List<UUID> findBadgeIdsByAdventureId(@Param("adventureId") UUID adventureId);
}
