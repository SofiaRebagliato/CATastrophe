package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Like;
import com.catastrophe.social.domain.port.in.LikeUseCase;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.LikeRepository;
import com.catastrophe.social.domain.port.out.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de likes.
 *
 * Restricción: un gato solo puede dar un like por post.
 * Al dar/quitar like se actualiza el contador desnormalizado del post.
 */
@Service
@Transactional
public class LikeService implements LikeUseCase {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final EventPublisher eventPublisher;

    public LikeService(LikeRepository likeRepository,
                       PostRepository postRepository,
                       EventPublisher eventPublisher) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void like(UUID postId, UUID catId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        // Idempotente: si ya existe el like, no hacer nada
        if (likeRepository.existsByPostIdAndCatId(postId, catId)) {
            return;
        }

        // No puedes darte like a ti mismo
        if (post.catId().equals(catId)) {
            throw new BusinessRuleViolationException(
                    "SELF_LIKE",
                    "¡No puedes darte like a ti mismo! Narcisismo gatuno detectado."
            );
        }

        var like = Like.create(postId, catId);
        likeRepository.save(like);

        // Incrementar contador desnormalizado
        postRepository.save(post.incrementLikes());

        // Emitir evento
        eventPublisher.publish(new PostLiked(
                UUID.randomUUID(),
                Instant.now(),
                catId,
                postId,
                post.catId() // postOwnerId
        ));
    }

    @Override
    public void unlike(UUID postId, UUID catId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (!likeRepository.existsByPostIdAndCatId(postId, catId)) {
            return; // Idempotente
        }

        likeRepository.deleteByPostIdAndCatId(postId, catId);

        // Decrementar contador desnormalizado
        postRepository.save(post.decrementLikes());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLiked(UUID postId, UUID catId) {
        return likeRepository.existsByPostIdAndCatId(postId, catId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByPostId(UUID postId) {
        return likeRepository.countByPostId(postId);
    }
}
