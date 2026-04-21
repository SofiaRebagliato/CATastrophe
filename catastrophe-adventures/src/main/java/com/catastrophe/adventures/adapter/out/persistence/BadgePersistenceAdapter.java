package com.catastrophe.adventures.adapter.out.persistence;

import com.catastrophe.adventures.adapter.out.persistence.mapper.BadgeMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaAdventureRewardRepository;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaBadgeRepository;
import com.catastrophe.adventures.domain.model.Badge;
import com.catastrophe.adventures.domain.port.out.BadgeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BadgePersistenceAdapter implements BadgeRepository {

    private final JpaBadgeRepository jpaBadgeRepository;
    private final JpaAdventureRewardRepository jpaRewardRepository;

    public BadgePersistenceAdapter(JpaBadgeRepository jpaBadgeRepository,
                                   JpaAdventureRewardRepository jpaRewardRepository) {
        this.jpaBadgeRepository = jpaBadgeRepository;
        this.jpaRewardRepository = jpaRewardRepository;
    }

    @Override
    public List<Badge> findAll() {
        return jpaBadgeRepository.findAll().stream().map(BadgeMapper::toDomain).toList();
    }

    @Override
    public Optional<Badge> findById(UUID id) {
        return jpaBadgeRepository.findById(id).map(BadgeMapper::toDomain);
    }

    @Override
    public List<UUID> findBadgeIdsByAdventure(UUID adventureId) {
        return jpaRewardRepository.findBadgeIdsByAdventureId(adventureId);
    }
}
