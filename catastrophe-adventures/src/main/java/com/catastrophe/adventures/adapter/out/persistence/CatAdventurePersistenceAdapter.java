package com.catastrophe.adventures.adapter.out.persistence;

import com.catastrophe.adventures.adapter.out.persistence.mapper.CatAdventureMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaCatAdventureRepository;
import com.catastrophe.adventures.domain.model.CatAdventure;
import com.catastrophe.adventures.domain.port.out.CatAdventureRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatAdventurePersistenceAdapter implements CatAdventureRepository {

    private final JpaCatAdventureRepository jpaRepository;

    public CatAdventurePersistenceAdapter(JpaCatAdventureRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CatAdventure save(CatAdventure catAdventure) {
        var entity = CatAdventureMapper.toEntity(catAdventure);
        var saved = jpaRepository.save(entity);
        return CatAdventureMapper.toDomain(saved);
    }

    @Override
    public Optional<CatAdventure> findById(UUID id) {
        return jpaRepository.findById(id).map(CatAdventureMapper::toDomain);
    }

    @Override
    public List<CatAdventure> findActiveByCatId(UUID catId) {
        return jpaRepository.findActiveByCatId(catId)
                .stream().map(CatAdventureMapper::toDomain).toList();
    }

    @Override
    public List<CatAdventure> findByCatId(UUID catId, int page, int size) {
        return jpaRepository.findByCatIdOrderByStartedAtDesc(catId, PageRequest.of(page, size))
                .stream().map(CatAdventureMapper::toDomain).toList();
    }

    @Override
    public boolean existsActive(UUID catId, UUID adventureId) {
        return jpaRepository.existsActiveByCatIdAndAdventureId(catId, adventureId);
    }
}
