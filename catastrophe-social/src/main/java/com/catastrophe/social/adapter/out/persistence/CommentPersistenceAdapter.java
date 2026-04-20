package com.catastrophe.social.adapter.out.persistence;

import com.catastrophe.social.adapter.out.persistence.mapper.CommentMapper;
import com.catastrophe.social.adapter.out.persistence.repository.JpaCommentRepository;
import com.catastrophe.social.domain.model.Comment;
import com.catastrophe.social.domain.port.out.CommentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CommentPersistenceAdapter implements CommentRepository {

    private final JpaCommentRepository jpaRepository;

    public CommentPersistenceAdapter(JpaCommentRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Comment save(Comment comment) {
        var entity = CommentMapper.toEntity(comment);
        return CommentMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return jpaRepository.findById(id).map(CommentMapper::toDomain);
    }

    @Override
    public List<Comment> findByPostId(UUID postId, int page, int size) {
        return jpaRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(page, size))
                .stream().map(CommentMapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
