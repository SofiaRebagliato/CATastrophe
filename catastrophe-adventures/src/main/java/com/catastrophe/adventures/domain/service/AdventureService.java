package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.Adventure;
import com.catastrophe.adventures.domain.model.CatAdventure;
import com.catastrophe.commons.event.CatastropheEvent.*;
import com.catastrophe.commons.exception.CatastropheExceptions.*;
import com.catastrophe.adventures.domain.port.in.AdventureUseCase;
import com.catastrophe.adventures.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de aventuras.
 *
 * Cuando un gato completa una aventura:
 * 1. Se actualiza el estado a COMPLETED
 * 2. Se otorgan los badges asociados
 * 3. Se publica un evento AdventureCompleted en Kafka
 * 4. Se actualiza el ranking en Redis
 */
@Service
@Transactional
public class AdventureService implements AdventureUseCase {

    private final AdventureRepository adventureRepository;
    private final CatAdventureRepository catAdventureRepository;
    private final BadgeRepository badgeRepository;
    private final CatBadgeRepository catBadgeRepository;
    private final EventPublisher eventPublisher;
    private final RankingCachePort rankingCache;

    public AdventureService(AdventureRepository adventureRepository,
                            CatAdventureRepository catAdventureRepository,
                            BadgeRepository badgeRepository,
                            CatBadgeRepository catBadgeRepository,
                            EventPublisher eventPublisher,
                            RankingCachePort rankingCache) {
        this.adventureRepository = adventureRepository;
        this.catAdventureRepository = catAdventureRepository;
        this.badgeRepository = badgeRepository;
        this.catBadgeRepository = catBadgeRepository;
        this.eventPublisher = eventPublisher;
        this.rankingCache = rankingCache;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Adventure> findAvailable(String type) {
        if (type != null && !type.isBlank()) {
            return adventureRepository.findByType(type);
        }
        return adventureRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Adventure> findById(UUID id) {
        return adventureRepository.findById(id);
    }

    @Override
    public CatAdventure start(StartAdventureCommand command) {
        var adventure = adventureRepository.findById(command.adventureId())
                .orElseThrow(() -> new ResourceNotFoundException("Adventure", command.adventureId()));

        // Validar disponibilidad
        if (!adventure.isAvailableAt(Instant.now())) {
            throw new BusinessRuleViolationException(
                    "ADVENTURE_NOT_AVAILABLE",
                    "Esta aventura no está disponible ahora. ¡Los gatos pacientes son recompensados!"
            );
        }

        // Validar que no tenga la misma aventura activa (a menos que sea repetible y no tenga una activa)
        if (catAdventureRepository.existsActive(command.catId(), command.adventureId())) {
            throw new BusinessRuleViolationException(
                    "ADVENTURE_ALREADY_ACTIVE",
                    "¡Ya estás en esta aventura! Termínala antes de empezar otra vez."
            );
        }

        var catAdventure = CatAdventure.start(command.catId(), command.adventureId());
        var saved = catAdventureRepository.save(catAdventure);

        // Publicar evento
        eventPublisher.publish(new AdventureStarted(
                UUID.randomUUID(), Instant.now(),
                command.catId(), command.adventureId(),
                adventure.difficulty()
        ));

        return saved;
    }

    @Override
    public CatAdventure updateProgress(UUID catAdventureId, UUID catId, int progressPct) {
        var catAdventure = findOwnedCatAdventure(catAdventureId, catId);
        var updated = catAdventure.updateProgress(progressPct);
        var saved = catAdventureRepository.save(updated);

        // Si se completó con el progreso, procesar completación
        if (saved.isCompleted() && !catAdventure.isCompleted()) {
            processCompletion(saved);
        }

        return saved;
    }

    @Override
    public CatAdventure complete(UUID catAdventureId, UUID catId) {
        var catAdventure = findOwnedCatAdventure(catAdventureId, catId);
        var completed = catAdventure.updateProgress(100);
        var saved = catAdventureRepository.save(completed);
        processCompletion(saved);
        return saved;
    }

    @Override
    public CatAdventure abandon(UUID catAdventureId, UUID catId) {
        var catAdventure = findOwnedCatAdventure(catAdventureId, catId);
        var abandoned = catAdventure.abandon();
        return catAdventureRepository.save(abandoned);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatAdventure> findActiveByCat(UUID catId) {
        return catAdventureRepository.findActiveByCatId(catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatAdventure> findHistoryByCat(UUID catId, int page, int size) {
        return catAdventureRepository.findByCatId(catId, page, size);
    }

    // ── Private helpers ──

    private CatAdventure findOwnedCatAdventure(UUID catAdventureId, UUID catId) {
        var catAdventure = catAdventureRepository.findById(catAdventureId)
                .orElseThrow(() -> new ResourceNotFoundException("CatAdventure", catAdventureId));

        if (!catAdventure.catId().equals(catId)) {
            throw new BusinessRuleViolationException(
                    "ADVENTURE_OWNERSHIP",
                    "¡Esa aventura no es tuya! Cada gato tiene sus propias misiones."
            );
        }

        if (catAdventure.isCompleted()) {
            throw new BusinessRuleViolationException(
                    "ADVENTURE_ALREADY_COMPLETED",
                    "¡Esta aventura ya está completada! Busca nuevas aventuras."
            );
        }

        return catAdventure;
    }

    private void processCompletion(CatAdventure catAdventure) {
        var adventure = adventureRepository.findById(catAdventure.adventureId()).orElseThrow();

        // Otorgar badges asociados a la aventura
        var badgeIds = badgeRepository.findBadgeIdsByAdventure(adventure.id());
        for (var badgeId : badgeIds) {
            if (!catBadgeRepository.exists(catAdventure.catId(), badgeId)) {
                catBadgeRepository.save(
                        com.catastrophe.adventures.domain.model.CatBadge.award(catAdventure.catId(), badgeId)
                );
                // Actualizar ranking de badges
                int badgeCount = catBadgeRepository.countByCatId(catAdventure.catId());
                rankingCache.updateScore("ranking:badges", catAdventure.catId(), badgeCount);
            }
        }

        // Publicar evento de aventura completada
        eventPublisher.publish(new AdventureCompleted(
                UUID.randomUUID(), Instant.now(),
                catAdventure.catId(), catAdventure.adventureId(),
                adventure.xpReward()
        ));
    }
}
