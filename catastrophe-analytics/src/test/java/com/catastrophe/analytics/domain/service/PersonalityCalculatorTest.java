package com.catastrophe.analytics.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.event.CatastropheEvent.BadgeEarned;
import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.CatFollowed;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.event.ChallengeResult;
import com.catastrophe.analytics.domain.model.Trait;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PersonalityCalculatorTest {

    private final PersonalityCalculator calc = new PersonalityCalculator();

    // ── smooth ──

    @Test
    void smooth_combines_current_and_impulse_with_alpha_weighting() {
        // alpha=0.15, current=0.5, impulse=1.0
        // → 0.15 * 1.0 + 0.85 * 0.5 = 0.575
        double result = calc.smooth(0.5, 1.0);
        assertThat(result).isCloseTo(0.575, within(0.0001));
    }

    @Test
    void smooth_with_zero_impulse_returns_current_unchanged() {
        assertThat(calc.smooth(0.42, 0.0)).isEqualTo(0.42);
    }

    @Test
    void smooth_clamps_to_unit_interval() {
        assertThat(calc.smooth(0.0, -5.0)).isGreaterThanOrEqualTo(0.0);
        assertThat(calc.smooth(1.0, 10.0)).isLessThanOrEqualTo(1.0);
    }

    @Test
    void smooth_converges_towards_impulse_after_repeated_application() {
        // Aplicar el mismo impulso muchas veces debe converger hacia el impulso
        double score = 0.0;
        for (int i = 0; i < 100; i++) {
            score = calc.smooth(score, 1.0);
        }
        assertThat(score).isCloseTo(1.0, within(0.001));
    }

    // ── impulseFor ──

    @Test
    void meowPosted_boosts_social() {
        var event = new MeowPosted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "PHOTO");
        var impulses = calc.impulseFor(event);

        assertThat(impulses).containsKey(Trait.SOCIAL);
        assertThat(impulses.get(Trait.SOCIAL)).isGreaterThan(0.0);
    }

    @Test
    void postLiked_boosts_social_for_recipient() {
        var event = new PostLiked(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var impulses = calc.impulseFor(event);

        assertThat(impulses).containsKey(Trait.SOCIAL);
    }

    @Test
    void catFollowed_boosts_social() {
        var event = new CatFollowed(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID());
        var impulses = calc.impulseFor(event);

        assertThat(impulses).containsKey(Trait.SOCIAL);
    }

    @Test
    void adventureCompleted_boosts_playful_and_hunter() {
        var event = new AdventureCompleted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), 50);
        var impulses = calc.impulseFor(event);

        assertThat(impulses).containsKeys(Trait.PLAYFUL, Trait.HUNTER);
        assertThat(impulses.get(Trait.PLAYFUL)).isEqualTo(1.0);
    }

    @Test
    void challengeWon_emphasizes_hunter() {
        var event = new ChallengeCompleted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ChallengeResult.WON, 100, 50);
        var impulses = calc.impulseFor(event);

        assertThat(impulses.get(Trait.HUNTER)).isEqualTo(1.0);
        assertThat(impulses.get(Trait.PLAYFUL)).isLessThan(impulses.get(Trait.HUNTER));
    }

    @Test
    void challengeLost_only_minor_playful_boost() {
        var event = new ChallengeCompleted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ChallengeResult.LOST, 50, 25);
        var impulses = calc.impulseFor(event);

        assertThat(impulses).doesNotContainKey(Trait.HUNTER);
        assertThat(impulses.get(Trait.PLAYFUL)).isLessThan(0.5);
    }

    @Test
    void legendaryBadge_strongly_boosts_mysterious() {
        var event = new BadgeEarned(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "Sombra Eterna", "LEGENDARY");
        var impulses = calc.impulseFor(event);

        assertThat(impulses.get(Trait.MYSTERIOUS)).isEqualTo(1.0);
    }

    @Test
    void commonBadge_weakly_boosts_mysterious() {
        var event = new BadgeEarned(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "Primer Maullido", "COMMON");
        var impulses = calc.impulseFor(event);

        assertThat(impulses.get(Trait.MYSTERIOUS)).isLessThan(0.5);
    }

    @Test
    void xpGained_without_level_up_no_impulse() {
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), 30, "challenge", 80, 1);
        var impulses = calc.impulseFor(event);

        assertThat(impulses).isEmpty();
    }

    @Test
    void xpGained_significant_level_boosts_playful() {
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), 100, "adventure", 500, 3);
        var impulses = calc.impulseFor(event);

        assertThat(impulses).containsKey(Trait.PLAYFUL);
    }

    @Test
    void ignored_events_return_empty_map() {
        var ignored = new java.util.ArrayList<com.catastrophe.commons.event.CatastropheEvent>();
        ignored.add(new CatCreated(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "Felix", "Siamese"));
        ignored.add(new AdventureStarted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "easy"));

        for (var event : ignored) {
            assertThat(calc.impulseFor(event))
                    .as("evento ignorado: %s", event.getClass().getSimpleName())
                    .isEmpty();
        }
    }
}
