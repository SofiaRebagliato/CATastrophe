package com.catastrophe.analytics.adapter.out.persistence.repository;

import com.catastrophe.analytics.adapter.out.persistence.entity.ProcessedPersonalityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaProcessedPersonalityEventRepository
        extends JpaRepository<ProcessedPersonalityEventEntity, UUID> {
}
