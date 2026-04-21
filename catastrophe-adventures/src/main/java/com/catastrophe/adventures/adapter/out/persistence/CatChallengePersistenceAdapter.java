package com.catastrophe.adventures.adapter.out.persistence;

import com.catastrophe.adventures.adapter.out.persistence.mapper.CatChallengeMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaCatChallengeRepository;
import com.catastrophe.adventures.domain.model.CatChallenge;
import com.catastrophe.adventures.domain.port.out.CatChallengeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatChallengePersistenceAdapter implements CatChallengeRepository {

    private final JpaCatChallengeRepository jpaRepository;

    public CatChallengePersistenceAdapter(JpaCatChallengeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CatChallenge save(CatChallenge catChallenge) {
        var entity = CatChallengeMapper.toEntity(catChallenge);
        var saved = jpaRepository.save(entity);
        return CatChallengeMapper.toDomain(saved);
    }

    @Override
    public Optional<CatChallenge> findById(UUID id) {
        return jpaRepository.findById(id).map(CatChallengeMapper::toDomain);
    }

    @Override
    public List<CatChallenge> findPendingByChallenge(UUID challengeId) {
        return jpaRepository.findPendingByChallengeId(challengeId)
                .stream().map(CatChallengeMapper::toDomain).toList();
    }

    @Override
    public List<CatChallenge> findByCatId(UUID catId) {
        return jpaRepository.findByCatIdOrderByCreatedAtDesc(catId)
                .stream().map(CatChallengeMapper::toDomain).toList();
    }
}
