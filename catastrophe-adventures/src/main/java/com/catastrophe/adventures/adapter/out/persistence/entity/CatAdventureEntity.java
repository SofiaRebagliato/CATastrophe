package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cat_adventures")
public class CatAdventureEntity {

    @Id
    private UUID id;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Column(name = "adventure_id", nullable = false)
    private UUID adventureId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "progress_pct", nullable = false)
    private int progressPct;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected CatAdventureEntity() {}

    public CatAdventureEntity(UUID id, UUID catId, UUID adventureId, String status,
                              int progressPct, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.catId = catId;
        this.adventureId = adventureId;
        this.status = status;
        this.progressPct = progressPct;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getCatId() { return catId; }
    public UUID getAdventureId() { return adventureId; }
    public String getStatus() { return status; }
    public int getProgressPct() { return progressPct; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
