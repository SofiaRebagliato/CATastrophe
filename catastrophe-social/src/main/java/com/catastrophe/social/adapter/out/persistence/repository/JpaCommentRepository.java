package com.catastrophe.social.adapter.out.persistence.repository;

import com.catastrophe.social.adapter.out.persistence.entity.CommentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);
}
