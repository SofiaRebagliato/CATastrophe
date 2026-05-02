package com.catastrophe.analytics.adapter.out.persistence.repository;

import com.catastrophe.analytics.adapter.out.persistence.entity.CatPersonalityEntity;
import com.catastrophe.analytics.domain.model.Trait;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaCatPersonalityRepository extends JpaRepository<CatPersonalityEntity, UUID> {

    List<CatPersonalityEntity> findByCatId(UUID catId);

    Optional<CatPersonalityEntity> findByCatIdAndTrait(UUID catId, Trait trait);
}
