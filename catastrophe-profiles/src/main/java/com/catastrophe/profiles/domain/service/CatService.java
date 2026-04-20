package com.catastrophe.profiles.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Cat;
import com.catastrophe.profiles.domain.port.in.CatUseCase;
import com.catastrophe.profiles.domain.port.out.CatAvatarProvider;
import com.catastrophe.profiles.domain.port.out.CatRepository;
import com.catastrophe.profiles.domain.port.out.EventPublisher;
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

    private final CatRepository catRepository;
    private final CatAvatarProvider avatarProvider;
    private final EventPublisher eventPublisher;

    public CatService(CatRepository catRepository,
                      CatAvatarProvider avatarProvider,
                      EventPublisher eventPublisher) {
        this.catRepository = catRepository;
        this.avatarProvider = avatarProvider;
        this.eventPublisher = eventPublisher;
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
                command.ageMonths(),
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
                command.ageMonths() != null ? command.ageMonths() : cat.ageMonths(),
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
}
