package com.catastrophe.profiles.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Cat;
import com.catastrophe.profiles.domain.port.in.CatUseCase;
import com.catastrophe.profiles.domain.port.out.CatAvatarProvider;
import com.catastrophe.profiles.domain.port.out.CatRepository;
import com.catastrophe.profiles.domain.port.out.EventPublisher;
import com.catastrophe.profiles.domain.port.out.ProcessedXpEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de gatos.
 *
 * Al crear un gato se obtiene un avatar de TheCatAPI y se emite
 * un evento CatCreated al bus de Kafka.
 */
@Service
@Transactional
public class CatService implements CatUseCase {

    private static final Logger log = LoggerFactory.getLogger(CatService.class);

    private final CatRepository catRepository;
    private final CatAvatarProvider avatarProvider;
    private final EventPublisher eventPublisher;
    private final ProcessedXpEventRepository processedXpEvents;

    public CatService(CatRepository catRepository,
                      CatAvatarProvider avatarProvider,
                      EventPublisher eventPublisher,
                      ProcessedXpEventRepository processedXpEvents) {
        this.catRepository = catRepository;
        this.avatarProvider = avatarProvider;
        this.eventPublisher = eventPublisher;
        this.processedXpEvents = processedXpEvents;
    }

    @Override
    public Cat create(CreateCatCommand command) {
        // Validar unicidad: un humano no puede tener dos gatos con el mismo nombre
        if (catRepository.existsByHumanIdAndName(command.humanId(), command.name())) {
            throw new BusinessRuleViolationException(
                    "UNIQUE_CAT_NAME",
                    "Ya tienes un gato llamado '%s'".formatted(command.name())
            );
        }

        // Obtener avatar de TheCatAPI (con fallback)
        String avatarUrl = avatarProvider.fetchRandomAvatar(command.breed())
                .orElse("https://cdn2.thecatapi.com/images/default.jpg");

        var cat = Cat.create(
                command.humanId(),
                command.name(),
                command.breed(),
                command.birthDate(),
                command.bio()
        ).withAvatar(avatarUrl);

        var saved = catRepository.save(cat);

        // Publicar evento
        eventPublisher.publish(new CatCreated(
                UUID.randomUUID(),
                Instant.now(),
                saved.id(),
                saved.humanId(),
                saved.name(),
                saved.breed()
        ));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cat> findById(UUID id) {
        return catRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cat> findByHumanId(UUID humanId) {
        return catRepository.findByHumanId(humanId);
    }

    @Override
    public Cat update(UUID catId, UpdateCatCommand command) {
        var cat = catRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Cat", catId));

        var updated = new Cat(
                cat.id(),
                cat.humanId(),
                command.name() != null ? command.name() : cat.name(),
                command.breed() != null ? command.breed() : cat.breed(),
                command.birthDate() != null ? command.birthDate() : cat.birthDate(),
                cat.avatarUrl(),
                command.bio() != null ? command.bio() : cat.bio(),
                cat.xp(),
                cat.level(),
                cat.mood(),
                cat.createdAt(),
                Instant.now()
        );

        return catRepository.save(updated);
    }

    @Override
    public Cat refreshAvatar(UUID catId) {
        var cat = catRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Cat", catId));

        String newAvatar = avatarProvider.fetchRandomAvatar(cat.breed())
                .orElse(cat.avatarUrl());

        return catRepository.save(cat.withAvatar(newAvatar));
    }

    @Override
    public void delete(UUID catId) {
        if (catRepository.findById(catId).isEmpty()) {
            throw new ResourceNotFoundException("Cat", catId);
        }
        catRepository.deleteById(catId);
    }

    @Override
    public Optional<Cat> applyXpGain(UUID eventId, UUID catId, int amount, String source) {
        // Idempotencia: si el evento ya se procesó, salir sin tocar nada.
        if (!processedXpEvents.markProcessed(eventId, catId, amount, source)) {
            log.debug("Evento XP {} ya procesado para gato {}, ignorando", eventId, catId);
            return Optional.empty();
        }

        // Si el gato no existe, dejamos el evento marcado como procesado igualmente
        // (no queremos reintentar indefinidamente un evento huérfano).
        var optCat = catRepository.findById(catId);
        if (optCat.isEmpty()) {
            log.warn("Evento XP {} para gato inexistente {}; se descarta", eventId, catId);
            return Optional.empty();
        }

        var cat = optCat.get();
        var updated = cat.addXp(amount);
        var saved = catRepository.save(updated);

        log.info("XP aplicada a gato {}: +{} ({}). Total: {}, Nivel: {}",
                catId, amount, source, saved.xp(), saved.level());

        // Publicamos XpGained para que adventures actualice rankings y notifications
        // detecte level-ups. Reutilizamos el eventId entrante como correlation key
        // sería lo ideal, pero el evento XpGained tiene su propia identidad.
        eventPublisher.publish(new XpGained(
                UUID.randomUUID(),
                Instant.now(),
                saved.id(),
                amount,
                source,
                saved.xp(),
                saved.level()
        ));

        return Optional.of(saved);
    }
}
