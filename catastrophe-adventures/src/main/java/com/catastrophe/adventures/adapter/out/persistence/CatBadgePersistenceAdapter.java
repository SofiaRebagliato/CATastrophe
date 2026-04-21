package com.catastrophe.adventures.adapter.out.persistence;

import com.catastrophe.adventures.adapter.out.persistence.mapper.CatBadgeMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaCatBadgeRepository;
import com.catastrophe.adventures.domain.model.CatBadge;
import com.catastrophe.adventures.domain.port.out.CatBadgeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CatBadgePersistenceAdapter implements CatBadgeRepository {

    private final JpaCatBadgeRepository jpaRepository;

    public CatBadgePersistenceAdapter(JpaCatBadgeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CatBadge save(CatBadge catBadge) {
        var entity = CatBadgeMapper.toEntity(catBadge);
        var saved = jpaRepository.save(entity);
        return CatBadgeMapper.toDomain(saved);
    }

    @Override
    public List<CatBadge> findByCatId(UUID catId) {
        return jpaRepository.findByCatIdOrderByEarnedAtDesc(catId)
                .stream().map(CatBadgeMapper::toDomain).toList();
    }

    @Override
    public boolean exists(UUID catId, UUID badgeId) {
        return jpaRepository.existsByCatIdAndBadgeId(catId, badgeId);
    }

    @Override
    public int countByCatId(UUID catId) {
        return jpaRepository.countByCatId(catId);
    }
}
