package com.catastrophe.notifications.adapter.out.persistence.repository;

import com.catastrophe.notifications.adapter.out.persistence.entity.CatLevelStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaCatLevelStateRepository extends JpaRepository<CatLevelStateEntity, UUID> {
}
