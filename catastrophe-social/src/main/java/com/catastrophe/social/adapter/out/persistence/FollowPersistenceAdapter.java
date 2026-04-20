package com.catastrophe.social.adapter.out.persistence;

import com.catastrophe.social.adapter.out.persistence.mapper.FollowMapper;
import com.catastrophe.social.adapter.out.persistence.repository.JpaFollowRepository;
import com.catastrophe.social.domain.model.Follow;
import com.catastrophe.social.domain.port.out.FollowRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FollowPersistenceAdapter implements FollowRepository {

    private final JpaFollowRepository jpaRepository;

    public FollowPersistenceAdapter(JpaFollowRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Follow save(Follow follow) {
        var entity = FollowMapper.toEntity(follow);
        return FollowMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId) {
        return jpaRepository.findByFollowerIdAndFollowedId(followerId, followedId)
                .map(FollowMapper::toDomain);
    }

    @Override
    public boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId) {
        return jpaRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Override
    public void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId) {
        jpaRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Override
    public List<UUID> findFollowedIds(UUID followerId) {
        return jpaRepository.findFollowedIdsByFollowerId(followerId);
    }

    @Override
    public List<Follow> findByFollowerId(UUID followerId) {
        return jpaRepository.findByFollowerId(followerId).stream()
                .map(FollowMapper::toDomain).toList();
    }

    @Override
    public List<Follow> findByFollowedId(UUID followedId) {
        return jpaRepository.findByFollowedId(followedId).stream()
                .map(FollowMapper::toDomain).toList();
    }

    @Override
    public int countByFollowedId(UUID followedId) {
        return jpaRepository.countByFollowedId(followedId);
    }

    @Override
    public int countByFollowerId(UUID followerId) {
        return jpaRepository.countByFollowerId(followerId);
    }
}
