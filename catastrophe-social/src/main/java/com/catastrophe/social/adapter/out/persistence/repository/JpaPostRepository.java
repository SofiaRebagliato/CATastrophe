package com.catastrophe.social.adapter.out.persistence.repository;

import com.catastrophe.social.adapter.out.persistence.entity.PostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaPostRepository extends JpaRepository<PostEntity, UUID> {

    List<PostEntity> findByCatIdOrderByCreatedAtDesc(UUID catId, Pageable pageable);

    @Query("SELECT p FROM PostEntity p WHERE p.catId IN :catIds ORDER BY p.createdAt DESC")
    List<PostEntity> findByCatIdInOrderByCreatedAtDesc(
            @Param("catIds") List<UUID> catIds, Pageable pageable);
}
