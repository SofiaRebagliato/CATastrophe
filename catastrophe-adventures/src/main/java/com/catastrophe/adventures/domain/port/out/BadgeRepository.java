package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.Badge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BadgeRepository {
    List<Badge> findAll();
    Optional<Badge> findById(UUID id);
    List<UUID> findBadgeIdsByAdventure(UUID adventureId);
}
