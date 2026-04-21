package com.catastrophe.adventures.adapter.out.persistence.repository;

import com.catastrophe.adventures.adapter.out.persistence.entity.AdventureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaAdventureRepository extends JpaRepository<AdventureEntity, UUID> {

    List<AdventureEntity> findByAdventureTypeOrderByDifficultyAsc(String adventureType);
}
