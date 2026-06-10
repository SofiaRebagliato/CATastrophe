package com.catastrophe.profiles.adapter.out.persistence.repository;

import com.catastrophe.profiles.adapter.out.persistence.entity.CatEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCatRepository extends JpaRepository<CatEntity, UUID> {

    List<CatEntity> findByHumanId(UUID humanId);

    boolean existsByHumanIdAndName(UUID humanId, String name);

    /** Búsqueda por nombre (case-insensitive, coincidencia parcial). */
    List<CatEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
