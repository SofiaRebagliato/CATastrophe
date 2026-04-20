package com.catastrophe.social.adapter.out.persistence;

import com.catastrophe.social.adapter.out.persistence.mapper.LikeMapper;
import com.catastrophe.social.adapter.out.persistence.repository.JpaLikeRepository;
import com.catastrophe.social.domain.model.Like;
import com.catastrophe.social.domain.port.out.LikeRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class LikePersistenceAdapter implements LikeRepository {

    private final JpaLikeRepository jpaRepository;

    public LikePersistenceAdapter(JpaLikeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Like save(Like like) {
        var entity = LikeMapper.toEntity(like);
        return LikeMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Like> findByPostIdAndCatId(UUID postId, UUID catId) {
        return jpaRepository.findByPostIdAndCatId(postId, catId).map(LikeMapper::toDomain);
    }

    @Override
    public boolean existsByPostIdAndCatId(UUID postId, UUID catId) {
        return jpaRepository.existsByPostIdAndCatId(postId, catId);
    }

    @Override
    public void deleteByPostIdAndCatId(UUID postId, UUID catId) {
        jpaRepository.deleteByPostIdAndCatId(postId, catId);
    }

    @Override
    public int countByPostId(UUID postId) {
        return jpaRepository.countByPostId(postId);
    }
}
