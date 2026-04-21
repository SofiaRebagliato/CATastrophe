package com.catastrophe.adventures.adapter.in.web;

import com.catastrophe.adventures.domain.model.Badge;
import com.catastrophe.adventures.domain.model.CatBadge;
import com.catastrophe.adventures.domain.port.in.BadgeUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Badges y logros.
 */
@RestController
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final BadgeUseCase badgeUseCase;

    public BadgeController(BadgeUseCase badgeUseCase) {
        this.badgeUseCase = badgeUseCase;
    }

    @GetMapping
    public ResponseEntity<List<BadgeResponse>> findAll() {
        var badges = badgeUseCase.findAll().stream()
                .map(BadgeResponse::from).toList();
        return ResponseEntity.ok(badges);
    }

    @GetMapping("/cat/{catId}")
    public ResponseEntity<List<CatBadgeResponse>> findByCat(@PathVariable UUID catId) {
        var catBadges = badgeUseCase.findByCat(catId).stream()
                .map(CatBadgeResponse::from).toList();
        return ResponseEntity.ok(catBadges);
    }

    // ── DTOs ──

    record BadgeResponse(UUID id, String name, String description, String iconUrl, String rarity) {
        static BadgeResponse from(Badge b) {
            return new BadgeResponse(b.id(), b.name(), b.description(), b.iconUrl(), b.rarity());
        }
    }

    record CatBadgeResponse(UUID id, UUID catId, UUID badgeId, Instant earnedAt) {
        static CatBadgeResponse from(CatBadge cb) {
            return new CatBadgeResponse(cb.id(), cb.catId(), cb.badgeId(), cb.earnedAt());
        }
    }
}
