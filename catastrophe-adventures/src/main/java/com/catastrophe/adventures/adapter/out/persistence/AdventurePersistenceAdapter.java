package com.catastrophe.adventures.adapter.out.persistence;

import com.catastrophe.adventures.adapter.out.persistence.mapper.AdventureMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaAdventureRepository;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaAdventureRewardRepository;
import com.catastrophe.adventures.domain.model.Adventure;
import com.catastrophe.adventures.domain.port.out.AdventureRepository;
import com.catastrophe.adventures.domain.port.out.BadgeRepository;
import com.catastrophe.adventures.adapter.out.persistence.mapper.BadgeMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaBadgeRepository;
import com.catastrophe.adventures.domain.model.Badge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdventurePersistenceAdapter implements AdventureRepository {

    private final JpaAdventureRepository jpaRepository;

    public AdventurePersistenceAdapter(JpaAdventureRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Adventure> findByType(String type) {
        return jpaRepository.findByAdventureTypeOrderByDifficultyAsc(type)
                .stream().map(AdventureMapper::toDomain).toList();
    }

    @Override
    public List<Adventure> findAll() {
        return jpaRepository.findAll()
                .stream().map(AdventureMapper::toDomain).toList();
    }

    @Override
    public Optional<Adventure> findById(UUID id) {
        return jpaRepository.findById(id).map(AdventureMapper::toDomain);
    }
}
