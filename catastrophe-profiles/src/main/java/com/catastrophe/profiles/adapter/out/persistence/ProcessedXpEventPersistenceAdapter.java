package com.catastrophe.profiles.adapter.out.persistence;

import com.catastrophe.profiles.adapter.out.persistence.entity.ProcessedXpEventEntity;
import com.catastrophe.profiles.adapter.out.persistence.repository.JpaProcessedXpEventRepository;
import com.catastrophe.profiles.domain.port.out.ProcessedXpEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de persistencia — Idempotencia del consumer XP.
 * <p>
 * Implementa {@link ProcessedXpEventRepository#markProcessed} apoyándose en
 * la PK de {@code processed_xp_events}: si el INSERT viola la PK (porque
 * ya existe un registro con ese {@code event_id}) lo capturamos y devolvemos
 * {@code false}. Es la forma más simple de obtener un "insert if not exists"
 * portable y atómico.
 */
@Component
public class ProcessedXpEventPersistenceAdapter implements ProcessedXpEventRepository {

    private final JpaProcessedXpEventRepository jpaRepository;

    public ProcessedXpEventPersistenceAdapter(JpaProcessedXpEventRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean markProcessed(UUID eventId, UUID catId, int amount, String source) {
        if (jpaRepository.existsById(eventId)) {
            return false;
        }
        try {
            jpaRepository.save(new ProcessedXpEventEntity(
                    eventId, catId, amount, source, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Race condition: otro consumer del mismo grupo acaba de insertarlo.
            // Tratamos como "ya procesado".
            return false;
        }
    }
}
