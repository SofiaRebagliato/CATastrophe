package com.catastrophe.social.adapter.out.persistence;

import com.catastrophe.social.adapter.out.persistence.mapper.PostMapper;
import com.catastrophe.social.adapter.out.persistence.repository.JpaPostRepository;
import com.catastrophe.social.domain.model.Post;
import com.catastrophe.social.domain.port.out.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida — Implementa el puerto PostRepository usando JPA.
 */
@Component
public class PostPersistenceAdapter implements PostRepository {

    private final JpaPostRepository jpaRepository;

    public PostPersistenceAdapter(JpaPostRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Post save(Post post) {
        var entity = PostMapper.toEntity(post);
        var saved = jpaRepository.save(entity);
        return PostMapper.toDomain(saved);
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return jpaRepository.findById(id).map(PostMapper::toDomain);
    }

    @Override
    public List<Post> findByCatId(UUID catId, int page, int size) {
        return jpaRepository.findByCatIdOrderByCreatedAtDesc(catId, PageRequest.of(page, size))
                .stream().map(PostMapper::toDomain).toList();
    }

    @Override
    public List<Post> findByCatIds(List<UUID> catIds, int page, int size) {
        return jpaRepository.findByCatIdInOrderByCreatedAtDesc(catIds, PageRequest.of(page, size))
                .stream().map(PostMapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
