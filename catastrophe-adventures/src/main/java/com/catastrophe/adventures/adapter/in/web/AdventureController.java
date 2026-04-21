package com.catastrophe.adventures.adapter.in.web;

import com.catastrophe.adventures.domain.model.Adventure;
import com.catastrophe.adventures.domain.model.CatAdventure;
import com.catastrophe.adventures.domain.port.in.AdventureUseCase;
import com.catastrophe.adventures.domain.port.in.AdventureUseCase.StartAdventureCommand;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Aventuras y misiones.
 */
@RestController
@RequestMapping("/api/v1/adventures")
public class AdventureController {

    private final AdventureUseCase adventureUseCase;

    public AdventureController(AdventureUseCase adventureUseCase) {
        this.adventureUseCase = adventureUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AdventureResponse>> findAvailable(
            @RequestParam(required = false) String type) {
        var adventures = adventureUseCase.findAvailable(type).stream()
                .map(AdventureResponse::from).toList();
        return ResponseEntity.ok(adventures);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdventureResponse> findById(@PathVariable UUID id) {
        return adventureUseCase.findById(id)
                .map(a -> ResponseEntity.ok(AdventureResponse.from(a)))
                .orElseThrow(() -> new ResourceNotFoundException("Adventure", id));
    }

    @PostMapping("/start")
    public ResponseEntity<CatAdventureResponse> start(
            @Valid @RequestBody StartAdventureRequest request,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var command = new StartAdventureCommand(catId, request.adventureId());
        var catAdventure = adventureUseCase.start(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CatAdventureResponse.from(catAdventure));
    }

    @PatchMapping("/{catAdventureId}/progress")
    public ResponseEntity<CatAdventureResponse> updateProgress(
            @PathVariable UUID catAdventureId,
            @Valid @RequestBody UpdateProgressRequest request,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var updated = adventureUseCase.updateProgress(catAdventureId, catId, request.progressPct());
        return ResponseEntity.ok(CatAdventureResponse.from(updated));
    }

    @PostMapping("/{catAdventureId}/complete")
    public ResponseEntity<CatAdventureResponse> complete(
            @PathVariable UUID catAdventureId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var completed = adventureUseCase.complete(catAdventureId, catId);
        return ResponseEntity.ok(CatAdventureResponse.from(completed));
    }

    @PostMapping("/{catAdventureId}/abandon")
    public ResponseEntity<CatAdventureResponse> abandon(
            @PathVariable UUID catAdventureId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var abandoned = adventureUseCase.abandon(catAdventureId, catId);
        return ResponseEntity.ok(CatAdventureResponse.from(abandoned));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CatAdventureResponse>> findActive(
            @RequestHeader("X-Cat-Id") UUID catId) {
        var active = adventureUseCase.findActiveByCat(catId).stream()
                .map(CatAdventureResponse::from).toList();
        return ResponseEntity.ok(active);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CatAdventureResponse>> findHistory(
            @RequestHeader("X-Cat-Id") UUID catId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var history = adventureUseCase.findHistoryByCat(catId, page, size).stream()
                .map(CatAdventureResponse::from).toList();
        return ResponseEntity.ok(history);
    }

    // ── DTOs ──

    record StartAdventureRequest(@NotNull UUID adventureId) {}

    record UpdateProgressRequest(
            @Min(0) @Max(100) int progressPct
    ) {}

    record AdventureResponse(
            UUID id, String title, String description, String difficulty,
            int xpReward, String adventureType, boolean repeatable,
            Instant availableFrom, Instant availableUntil
    ) {
        static AdventureResponse from(Adventure a) {
            return new AdventureResponse(a.id(), a.title(), a.description(), a.difficulty(),
                    a.xpReward(), a.adventureType(), a.repeatable(),
                    a.availableFrom(), a.availableUntil());
        }
    }

    record CatAdventureResponse(
            UUID id, UUID catId, UUID adventureId, String status,
            int progressPct, Instant startedAt, Instant completedAt
    ) {
        static CatAdventureResponse from(CatAdventure ca) {
            return new CatAdventureResponse(ca.id(), ca.catId(), ca.adventureId(),
                    ca.status().dbValue(), ca.progressPct(), ca.startedAt(), ca.completedAt());
        }
    }
}
