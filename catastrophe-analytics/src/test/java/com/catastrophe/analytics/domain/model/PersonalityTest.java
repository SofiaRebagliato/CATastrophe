package com.catastrophe.analytics.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalityTest {

    @Test
    void empty_initializes_all_traits_to_zero() {
        var p = Personality.empty(UUID.randomUUID());

        for (var trait : Trait.values()) {
            assertThat(p.scoreOf(trait)).isEqualTo(0.0);
        }
    }

    @Test
    void empty_personality_has_no_dominant_trait() {
        var p = Personality.empty(UUID.randomUUID());

        assertThat(p.dominantTrait()).isEmpty();
    }

    @Test
    void dominantTrait_returns_highest_scored() {
        var scores = new EnumMap<Trait, Double>(Trait.class);
        scores.put(Trait.PLAYFUL, 0.3);
        scores.put(Trait.HUNTER, 0.9);
        scores.put(Trait.SOCIAL, 0.5);
        scores.put(Trait.LAZY, 0.0);
        scores.put(Trait.MYSTERIOUS, 0.1);

        var p = new Personality(UUID.randomUUID(), scores, Instant.now());

        assertThat(p.dominantTrait()).contains(Trait.HUNTER);
    }

    @Test
    void scores_map_is_immutable_after_construction() {
        var scores = new EnumMap<Trait, Double>(Trait.class);
        scores.put(Trait.PLAYFUL, 0.5);
        var p = new Personality(UUID.randomUUID(), scores, Instant.now());

        // El record copia el mapa para garantizar inmutabilidad
        assertThatThrownBy(() -> p.scores().put(Trait.HUNTER, 0.9))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejects_null_required_fields() {
        assertThatThrownBy(() ->
                new Personality(null, new EnumMap<>(Trait.class), Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }
}
