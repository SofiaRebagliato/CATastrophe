package com.catastrophe.notifications.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Estado mínimo persistido para detectar level-ups: el último nivel conocido
 * de cada gato. Se actualiza con cada {@code XpGained} consumido.
 */
@Entity
@Table(name = "cat_level_state")
public class CatLevelStateEntity {

    @Id
    @Column(name = "cat_id")
    private UUID catId;

    @Column(name = "last_level", nullable = false)
    private int lastLevel;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatLevelStateEntity() {}

    public CatLevelStateEntity(UUID catId, int lastLevel, Instant updatedAt) {
        this.catId = catId;
        this.lastLevel = lastLevel;
        this.updatedAt = updatedAt;
    }

    public UUID getCatId() { return catId; }
    public void setCatId(UUID catId) { this.catId = catId; }
    public int getLastLevel() { return lastLevel; }
    public void setLastLevel(int lastLevel) { this.lastLevel = lastLevel; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
