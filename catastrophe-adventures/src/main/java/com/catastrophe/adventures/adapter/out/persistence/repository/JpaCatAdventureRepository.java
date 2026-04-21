package com.catastrophe.adventures.adapter.out.persistence.repository;

import com.catastrophe.adventures.adapter.out.persistence.entity.CatAdventureEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaCatAdventureRepository extends JpaRepository<CatAdventureEntity, UUID> {

    @Query("SELECT ca FROM CatAdventureEntity ca WHERE ca.catId = :catId AND ca.status = 'in_progress'")
    List<CatAdventureEntity> findActiveByCatId(@Param("catId") UUID catId);

    List<CatAdventureEntity> findByCatIdOrderByStartedAtDesc(UUID catId, Pageable pageable);

    @Query("SELECT COUNT(ca) > 0 FROM CatAdventureEntity ca " +
           "WHERE ca.catId = :catId AND ca.adventureId = :adventureId AND ca.status = 'in_progress'")
    boolean existsActiveByCatIdAndAdventureId(@Param("catId") UUID catId,
                                               @Param("adventureId") UUID adventureId);
}
