package com.catastrophe.notifications.adapter.out.persistence;

import com.catastrophe.notifications.adapter.out.persistence.entity.CatLevelStateEntity;
import com.catastrophe.notifications.adapter.out.persistence.repository.JpaCatLevelStateRepository;
import com.catastrophe.notifications.domain.port.out.CatLevelTrackerRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatLevelTrackerPersistenceAdapter implements CatLevelTrackerRepository {

    private final JpaCatLevelStateRepository jpaRepository;

    public CatLevelTrackerPersistenceAdapter(JpaCatLevelStateRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Integer> findLastLevel(UUID catId) {
        return jpaRepository.findById(catId).map(CatLevelStateEntity::getLastLevel);
    }

    @Override
    public void upsert(UUID catId, int level) {
        var entity = jpaRepository.findById(catId)
                .orElseGet(() -> new CatLevelStateEntity(catId, level, Instant.now()));
        entity.setLastLevel(level);
        entity.setUpdatedAt(Instant.now());
        jpaRepository.save(entity);
    }
}
