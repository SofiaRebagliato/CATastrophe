package com.catastrophe.adventures.adapter.out.persistence;

import com.catastrophe.adventures.adapter.out.persistence.mapper.ChallengeMapper;
import com.catastrophe.adventures.adapter.out.persistence.repository.JpaChallengeRepository;
import com.catastrophe.adventures.domain.model.Challenge;
import com.catastrophe.adventures.domain.port.out.ChallengeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ChallengePersistenceAdapter implements ChallengeRepository {

    private final JpaChallengeRepository jpaRepository;

    public ChallengePersistenceAdapter(JpaChallengeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Challenge> findAll() {
        return jpaRepository.findAll().stream().map(ChallengeMapper::toDomain).toList();
    }

    @Override
    public Optional<Challenge> findById(UUID id) {
        return jpaRepository.findById(id).map(ChallengeMapper::toDomain);
    }
}
