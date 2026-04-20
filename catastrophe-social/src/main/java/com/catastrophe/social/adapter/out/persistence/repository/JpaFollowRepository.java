package com.catastrophe.social.adapter.out.persistence.repository;

import com.catastrophe.social.adapter.out.persistence.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaFollowRepository extends JpaRepository<FollowEntity, UUID> {

    Optional<FollowEntity> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    List<FollowEntity> findByFollowerId(UUID followerId);

    List<FollowEntity> findByFollowedId(UUID followedId);

    @Query("SELECT f.followedId FROM FollowEntity f WHERE f.followerId = :followerId")
    List<UUID> findFollowedIdsByFollowerId(@Param("followerId") UUID followerId);

    int countByFollowedId(UUID followedId);

    int countByFollowerId(UUID followerId);
}
