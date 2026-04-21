package com.catastrophe.adventures.adapter.in.web;

import com.catastrophe.adventures.domain.model.RankingEntry;
import com.catastrophe.adventures.domain.port.in.RankingUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Rankings en tiempo real.
 */
@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingUseCase rankingUseCase;

    public RankingController(RankingUseCase rankingUseCase) {
        this.rankingUseCase = rankingUseCase;
    }

    @GetMapping("/global")
    public ResponseEntity<List<RankingEntryResponse>> getGlobalRanking(
            @RequestParam(defaultValue = "10") int top) {
        var ranking = rankingUseCase.getGlobalRanking(top).stream()
                .map(RankingEntryResponse::from).toList();
        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/badges")
    public ResponseEntity<List<RankingEntryResponse>> getBadgeRanking(
            @RequestParam(defaultValue = "10") int top) {
        var ranking = rankingUseCase.getBadgeRanking(top).stream()
                .map(RankingEntryResponse::from).toList();
        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/cat/{catId}")
    public ResponseEntity<RankingEntryResponse> getCatRank(@PathVariable UUID catId) {
        var rank = rankingUseCase.getCatRank(catId);
        return ResponseEntity.ok(RankingEntryResponse.from(rank));
    }

    // ── DTOs ──

    record RankingEntryResponse(int rank, UUID catId, double score) {
        static RankingEntryResponse from(RankingEntry r) {
            return new RankingEntryResponse(r.rank(), r.catId(), r.score());
        }
    }
}
