package com.catastrophe.analytics.adapter.out.persistence.entity;

import com.catastrophe.analytics.domain.model.Trait;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cat_personalities",
        uniqueConstraints = @UniqueConstraint(
                name = "idx_cat_personalities_cat_trait",
                columnNames = {"cat_id", "trait"}))
public class CatPersonalityEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "cat_id", nullable = false)
    private UUID catId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trait", nullable = false, length = 30)
    private Trait trait;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatPersonalityEntity() {}

    public CatPersonalityEntity(UUID id, UUID catId, Trait trait, double score, Instant updatedAt) {
        this.id = id;
        this.catId = catId;
        this.trait = trait;
        this.score = score;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCatId() { return catId; }
    public void setCatId(UUID catId) { this.catId = catId; }
    public Trait getTrait() { return trait; }
    public void setTrait(Trait trait) { this.trait = trait; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
