package com.catastrophe.analytics.adapter.out.persistence;

import com.catastrophe.analytics.adapter.out.persistence.entity.ProcessedPersonalityEventEntity;
import com.catastrophe.analytics.adapter.out.persistence.repository.JpaProcessedPersonalityEventRepository;
import com.catastrophe.analytics.domain.port.out.ProcessedPersonalityEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProcessedPersonalityEventAdapter implements ProcessedPersonalityEventRepository {

    private final JpaProcessedPersonalityEventRepository jpaRepository;

    public ProcessedPersonalityEventAdapter(JpaProcessedPersonalityEventRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean markProcessed(UUID eventId, UUID catId, String eventType) {
        if (jpaRepository.existsById(eventId)) {
            return false;
        }
        try {
            jpaRepository.save(new ProcessedPersonalityEventEntity(
                    eventId, catId, eventType, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Race condition: lo trata como ya procesado.
            return false;
        }
    }
}
