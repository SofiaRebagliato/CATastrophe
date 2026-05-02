package com.catastrophe.analytics.adapter.in.web;

import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.model.Trait;
import com.catastrophe.analytics.domain.port.in.PersonalityUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints REST de personalidades.
 */
@RestController
@RequestMapping("/api/v1/personalities")
public class PersonalityController {

    private final PersonalityUseCase useCase;

    public PersonalityController(PersonalityUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{catId}")
    public ResponseEntity<PersonalityResponse> get(@PathVariable UUID catId) {
        return ResponseEntity.ok(PersonalityResponse.from(useCase.findByCatId(catId)));
    }

    @GetMapping("/{catId}/dominant")
    public ResponseEntity<DominantTraitResponse> dominant(@PathVariable UUID catId) {
        var personality = useCase.findByCatId(catId);
        return ResponseEntity.ok(new DominantTraitResponse(
                catId,
                personality.dominantTrait().orElse(null),
                personality.dominantTrait().map(personality::scoreOf).orElse(0.0)
        ));
    }

    public record PersonalityResponse(
            UUID catId,
            Map<Trait, Double> scores,
            Trait dominant,
            Instant updatedAt
    ) {
        static PersonalityResponse from(Personality p) {
            return new PersonalityResponse(
                    p.catId(),
                    p.scores(),
                    p.dominantTrait().orElse(null),
                    p.updatedAt()
            );
        }
    }

    public record DominantTraitResponse(UUID catId, Trait trait, double score) {}
}
