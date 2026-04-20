package com.catastrophe.profiles.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cats")
public class CatEntity {

    @Id
    private UUID id;

    @Column(name = "human_id", nullable = false)
    private UUID humanId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String breed;

    @Column(name = "age_months")
    private Integer ageMonths;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private int level;

    @Column(length = 50)
    private String mood;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected CatEntity() {}

    public CatEntity(UUID id, UUID humanId, String name, String breed, Integer ageMonths,
                     String avatarUrl, String bio, int xp, int level, String mood,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.humanId = humanId;
        this.name = name;
        this.breed = breed;
        this.ageMonths = ageMonths;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.xp = xp;
        this.level = level;
        this.mood = mood;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getHumanId() { return humanId; }
    public String getName() { return name; }
    public String getBreed() { return breed; }
    public Integer getAgeMonths() { return ageMonths; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public String getMood() { return mood; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
