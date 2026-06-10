package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.Badge;
import com.catastrophe.adventures.domain.model.CatBadge;
import com.catastrophe.commons.event.CatastropheEvent.BadgeEarned;
import com.catastrophe.commons.exception.CatastropheExceptions.*;
import com.catastrophe.adventures.domain.port.in.BadgeUseCase;
import com.catastrophe.adventures.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BadgeService implements BadgeUseCase {

    private final BadgeRepository badgeRepository;
    private final CatBadgeRepository catBadgeRepository;
    private final EventPublisher eventPublisher;
    private final RankingCachePort rankingCache;

    public BadgeService(BadgeRepository badgeRepository,
                        CatBadgeRepository catBadgeRepository,
                        EventPublisher eventPublisher,
                        RankingCachePort rankingCache) {
        this.badgeRepository = badgeRepository;
        this.catBadgeRepository = catBadgeRepository;
        this.eventPublisher = eventPublisher;
        this.rankingCache = rankingCache;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Badge> findAll() {
        return badgeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Badge> findById(UUID badgeId) {
        return badgeRepository.findById(badgeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatBadge> findByCat(UUID catId) {
        return catBadgeRepository.findByCatId(catId);
    }

    @Override
    public CatBadge award(UUID catId, UUID badgeId) {
        var badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge", badgeId));

        if (catBadgeRepository.exists(catId, badgeId)) {
            throw new DuplicateResourceException("CatBadge", "cat_id+badge_id",
                    catId + "+" + badgeId);
        }

        var catBadge = CatBadge.award(catId, badgeId);
        var saved = catBadgeRepository.save(catBadge);

        // Actualizar ranking de badges
        int badgeCount = catBadgeRepository.countByCatId(catId);
        rankingCache.updateScore("ranking:badges", catId, badgeCount);

        // Publicar evento
        eventPublisher.publish(new BadgeEarned(
                UUID.randomUUID(), Instant.now(),
                catId, badgeId, badge.name(), badge.rarity()
        ));

        return saved;
    }
}
