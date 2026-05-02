package com.catastrophe.profiles.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de eventos XP ya procesados (idempotencia del consumer Kafka).
 * El {@code event_id} es la PK: un INSERT que viole la PK indica que el
 * evento ya estaba registrado, y por tanto no debe volver a aplicarse XP.
 */
@Entity
@Table(name = "processed_xp_events")
public class ProcessedXpEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedXpEventEntity() {}

    public ProcessedXpEventEntity(UUID eventId, UUID catId, int amount, String source, Instant processedAt) {
        this.eventId = eventId;
        this.catId = catId;
        this.amount = amount;
        this.source = source;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public UUID getCatId() { return catId; }
    public int getAmount() { return amount; }
    public String getSource() { return source; }
    public Instant getProcessedAt() { return processedAt; }
}
