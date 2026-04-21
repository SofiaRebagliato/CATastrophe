package com.catastrophe.adventures.adapter.out.persistence.repository;

import com.catastrophe.adventures.adapter.out.persistence.entity.CatBadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCatBadgeRepository extends JpaRepository<CatBadgeEntity, UUID> {

    List<CatBadgeEntity> findByCatIdOrderByEarnedAtDesc(UUID catId);

    boolean existsByCatIdAndBadgeId(UUID catId, UUID badgeId);

    int countByCatId(UUID catId);
}
