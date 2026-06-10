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
    public ResponseEntity<List<EnrichedCatBadgeResponse>> findByCat(@PathVariable UUID catId) {
        var catBadges = badgeUseCase.findByCat(catId);
        var enriched = catBadges.stream()
                .map(cb -> {
                    var badge = badgeUseCase.findById(cb.badgeId());
                    return new EnrichedCatBadgeResponse(
                            cb.id(),
                            cb.catId(),
                            cb.badgeId(),
                            cb.earnedAt(),
                            badge.map(Badge::name).orElse("Insignia"),
                            badge.map(Badge::description).orElse(""),
                            badge.map(Badge::iconUrl).orElse("🎖️"),
                            badge.map(Badge::rarity).orElse("common")
                    );
                })
                .toList();
        return ResponseEntity.ok(enriched);
    }

    // ── DTOs ──

    record BadgeResponse(UUID id, String name, String description, String iconUrl, String rarity) {
        static BadgeResponse from(Badge b) {
            return new BadgeResponse(b.id(), b.name(), b.description(), b.iconUrl(), b.rarity());
        }
    }

    record EnrichedCatBadgeResponse(
            UUID id,
            UUID catId,
            UUID badgeId,
            Instant earnedAt,
            String name,
            String description,
            String iconUrl,
            String rarity
    ) {}

    // Mantenemos CatBadgeResponse por compatibilidad si se usa en otro lado
    record CatBadgeResponse(UUID id, UUID catId, UUID badgeId, Instant earnedAt) {
        static CatBadgeResponse from(CatBadge cb) {
            return new CatBadgeResponse(cb.id(), cb.catId(), cb.badgeId(), cb.earnedAt());
        }
    }
}
