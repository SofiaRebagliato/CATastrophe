package com.catastrophe.profiles.adapter.out.persistence.repository;

import com.catastrophe.profiles.adapter.out.persistence.entity.ProcessedXpEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaProcessedXpEventRepository extends JpaRepository<ProcessedXpEventEntity, UUID> {
}
