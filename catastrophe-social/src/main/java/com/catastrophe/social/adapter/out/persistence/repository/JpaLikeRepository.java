package com.catastrophe.social.adapter.out.persistence.repository;

import com.catastrophe.social.adapter.out.persistence.entity.LikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaLikeRepository extends JpaRepository<LikeEntity, UUID> {

    Optional<LikeEntity> findByPostIdAndCatId(UUID postId, UUID catId);

    boolean existsByPostIdAndCatId(UUID postId, UUID catId);

    void deleteByPostIdAndCatId(UUID postId, UUID catId);

    int countByPostId(UUID postId);
}
