package com.catastrophe.analytics.adapter.out.persistence;

import com.catastrophe.analytics.adapter.out.persistence.entity.CatPersonalityEntity;
import com.catastrophe.analytics.adapter.out.persistence.repository.JpaCatPersonalityRepository;
import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.model.Trait;
import com.catastrophe.analytics.domain.port.out.PersonalityRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.UUID;

/**
 * Adaptador de persistencia para personalidades.
 * <p>
 * El upsert se hace en dos pasos (find + save) en lugar de con SQL nativo
 * para mantener portabilidad y simpleza. La unicidad la garantiza el índice
 * único de la BD; en caso de carrera, JPA propagaría una violación que
 * el consumer manejaría como cualquier otra excepción.
 */
@Component
public class PersonalityPersistenceAdapter implements PersonalityRepository {

    private final JpaCatPersonalityRepository jpaRepository;

    public PersonalityPersistenceAdapter(JpaCatPersonalityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Personality findByCatId(UUID catId) {
        var rows = jpaRepository.findByCatId(catId);
        if (rows.isEmpty()) {
            return Personality.empty(catId);
        }

        var scores = new EnumMap<Trait, Double>(Trait.class);
        // Inicializar todos los traits a 0 para garantizar mapa completo
        for (var t : Trait.values()) {
            scores.put(t, 0.0);
        }
        Instant latestUpdate = Instant.EPOCH;
        for (var row : rows) {
            scores.put(row.getTrait(), row.getScore());
            if (row.getUpdatedAt().isAfter(latestUpdate)) {
                latestUpdate = row.getUpdatedAt();
            }
        }
        return new Personality(catId, scores, latestUpdate);
    }

    @Override
    public void upsertScore(UUID catId, Trait trait, double score) {
        var existing = jpaRepository.findByCatIdAndTrait(catId, trait);
        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setScore(score);
            entity.setUpdatedAt(Instant.now());
            jpaRepository.save(entity);
        } else {
            jpaRepository.save(new CatPersonalityEntity(
                    UUID.randomUUID(), catId, trait, score, Instant.now()));
        }
    }
}
