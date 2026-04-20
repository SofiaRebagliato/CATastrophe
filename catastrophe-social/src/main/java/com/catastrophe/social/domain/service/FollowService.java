package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.CatFollowed;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.social.domain.model.Follow;
import com.catastrophe.social.domain.port.in.FollowUseCase;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.FollowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de seguimientos.
 *
 * Un gato no puede seguirse a sí mismo.
 * Cada follow genera un evento Kafka (para notificaciones).
 */
@Service
@Transactional
public class FollowService implements FollowUseCase {

    private final FollowRepository followRepository;
    private final EventPublisher eventPublisher;

    public FollowService(FollowRepository followRepository,
                         EventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Follow follow(UUID followerId, UUID followedId) {
        if (followerId.equals(followedId)) {
            throw new BusinessRuleViolationException(
                    "SELF_FOLLOW",
                    "¡Un gato no puede seguirse a sí mismo! Aunque sabemos que se admiran mucho."
            );
        }

        // Idempotente
        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return followRepository.findByFollowerIdAndFollowedId(followerId, followedId)
                    .orElseThrow();
        }

        var follow = Follow.create(followerId, followedId);
        var saved = followRepository.save(follow);

        // Emitir evento
        eventPublisher.publish(new CatFollowed(
                UUID.randomUUID(),
                Instant.now(),
                followerId,
                followedId
        ));

        return saved;
    }

    @Override
    public void unfollow(UUID followerId, UUID followedId) {
        followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followedId) {
        return followRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Follow> getFollowing(UUID catId) {
        return followRepository.findByFollowerId(catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Follow> getFollowers(UUID catId) {
        return followRepository.findByFollowedId(catId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countFollowers(UUID catId) {
        return followRepository.countByFollowedId(catId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countFollowing(UUID catId) {
        return followRepository.countByFollowerId(catId);
    }
}
