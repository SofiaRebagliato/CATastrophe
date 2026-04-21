package com.catastrophe.adventures.adapter.in.web;

import com.catastrophe.adventures.domain.model.CatChallenge;
import com.catastrophe.adventures.domain.model.Challenge;
import com.catastrophe.adventures.domain.port.in.ChallengeUseCase;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Retos PvP entre gatos.
 */
@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

    private final ChallengeUseCase challengeUseCase;

    public ChallengeController(ChallengeUseCase challengeUseCase) {
        this.challengeUseCase = challengeUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> findAll() {
        var challenges = challengeUseCase.findAll().stream()
                .map(ChallengeResponse::from).toList();
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeResponse> findById(@PathVariable UUID id) {
        return challengeUseCase.findById(id)
                .map(c -> ResponseEntity.ok(ChallengeResponse.from(c)))
                .orElseThrow(() -> new ResourceNotFoundException("Challenge", id));
    }

    @PostMapping("/create")
    public ResponseEntity<CatChallengeResponse> create(
            @Valid @RequestBody CreateChallengeRequest request,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var catChallenge = challengeUseCase.create(catId, request.challengeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CatChallengeResponse.from(catChallenge));
    }

    @PostMapping("/{catChallengeId}/accept")
    public ResponseEntity<CatChallengeResponse> accept(
            @PathVariable UUID catChallengeId,
            @RequestHeader("X-Cat-Id") UUID opponentId) {
        var accepted = challengeUseCase.accept(catChallengeId, opponentId);
        return ResponseEntity.ok(CatChallengeResponse.from(accepted));
    }

    @PostMapping("/{catChallengeId}/resolve")
    public ResponseEntity<ResolveResponse> resolve(
            @PathVariable UUID catChallengeId,
            @Valid @RequestBody ResolveRequest request) {
        var result = challengeUseCase.resolve(
                catChallengeId, request.challengerScore(), request.opponentScore());
        return ResponseEntity.ok(new ResolveResponse(
                CatChallengeResponse.from(result.challenger()),
                CatChallengeResponse.from(result.opponent())
        ));
    }

    @GetMapping("/{challengeId}/pending")
    public ResponseEntity<List<CatChallengeResponse>> findPending(
            @PathVariable UUID challengeId) {
        var pending = challengeUseCase.findPending(challengeId).stream()
                .map(CatChallengeResponse::from).toList();
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/my")
    public ResponseEntity<List<CatChallengeResponse>> findMyChallenges(
            @RequestHeader("X-Cat-Id") UUID catId) {
        var challenges = challengeUseCase.findByCat(catId).stream()
                .map(CatChallengeResponse::from).toList();
        return ResponseEntity.ok(challenges);
    }

    // ── DTOs ──

    record CreateChallengeRequest(@NotNull UUID challengeId) {}

    record ResolveRequest(
            @Min(0) int challengerScore,
            @Min(0) int opponentScore
    ) {}

    record ChallengeResponse(
            UUID id, String title, String description, String challengeType,
            int xpReward, Instant startsAt, Instant endsAt
    ) {
        static ChallengeResponse from(Challenge c) {
            return new ChallengeResponse(c.id(), c.title(), c.description(),
                    c.challengeType(), c.xpReward(), c.startsAt(), c.endsAt());
        }
    }

    record CatChallengeResponse(
            UUID id, UUID catId, UUID challengeId, UUID opponentId,
            String status, int score, Instant createdAt
    ) {
        static CatChallengeResponse from(CatChallenge cc) {
            return new CatChallengeResponse(cc.id(), cc.catId(), cc.challengeId(),
                    cc.opponentId(), cc.status().dbValue(), cc.score(), cc.createdAt());
        }
    }

    record ResolveResponse(CatChallengeResponse challenger, CatChallengeResponse opponent) {}
}
