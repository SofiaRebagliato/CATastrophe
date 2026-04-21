package com.catastrophe.adventures.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "badges")
public class BadgeEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(nullable = false, length = 20)
    private String rarity;

    protected BadgeEntity() {}

    public BadgeEntity(UUID id, String name, String description, String iconUrl, String rarity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.rarity = rarity;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconUrl() { return iconUrl; }
    public String getRarity() { return rarity; }
}
