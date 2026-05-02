package com.catastrophe.notifications.domain.service;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.event.CatastropheEvent.BadgeEarned;
import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.CatFollowed;
import com.catastrophe.commons.event.CatastropheEvent.CatUpdated;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.event.CatastropheEvent.PostCommented;
import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.event.ChallengeResult;
import com.catastrophe.notifications.domain.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del factory: una rama por cada tipo de evento del sealed.
 * Si en el futuro se añade un evento al sealed, este test no rompe pero
 * el switch del factory sí — el compilador exigirá una decisión.
 */
class NotificationFactoryTest {

    private final NotificationFactory factory = new NotificationFactory();

    // ── Sociales ──

    @Test
    void postLiked_notifies_post_owner() {
        var liker = UUID.randomUUID();
        var owner = UUID.randomUUID();
        var postId = UUID.randomUUID();
        var event = new PostLiked(UUID.randomUUID(), Instant.now(), liker, postId, owner);

        var n = factory.from(event).orElseThrow();

        assertThat(n.recipientCatId()).isEqualTo(owner);
        assertThat(n.type()).isEqualTo(NotificationType.POST_LIKED);
        assertThat(n.payload()).containsEntry("postId", postId.toString())
                               .containsEntry("likerCatId", liker.toString());
    }

    @Test
    void postCommented_notifies_post_owner() {
        var commenter = UUID.randomUUID();
        var owner = UUID.randomUUID();
        var postId = UUID.randomUUID();
        var commentId = UUID.randomUUID();
        var event = new PostCommented(UUID.randomUUID(), Instant.now(),
                commenter, postId, owner, commentId);

        var n = factory.from(event).orElseThrow();

        assertThat(n.recipientCatId()).isEqualTo(owner);
        assertThat(n.type()).isEqualTo(NotificationType.POST_COMMENTED);
        assertThat(n.payload())
                .containsEntry("postId", postId.toString())
                .containsEntry("commentId", commentId.toString())
                .containsEntry("commenterCatId", commenter.toString());
    }

    @Test
    void catFollowed_notifies_followed_cat() {
        var follower = UUID.randomUUID();
        var followed = UUID.randomUUID();
        var event = new CatFollowed(UUID.randomUUID(), Instant.now(), follower, followed);

        var n = factory.from(event).orElseThrow();

        assertThat(n.recipientCatId()).isEqualTo(followed);
        assertThat(n.type()).isEqualTo(NotificationType.CAT_FOLLOWED);
        assertThat(n.payload()).containsEntry("followerCatId", follower.toString());
    }

    // ── Gamificación ──

    @Test
    void adventureCompleted_notifies_actor_with_xp() {
        var catId = UUID.randomUUID();
        var advId = UUID.randomUUID();
        var event = new AdventureCompleted(UUID.randomUUID(), Instant.now(), catId, advId, 75);

        var n = factory.from(event).orElseThrow();

        assertThat(n.recipientCatId()).isEqualTo(catId);
        assertThat(n.type()).isEqualTo(NotificationType.ADVENTURE_COMPLETED);
        assertThat(n.message()).contains("75");
        assertThat(n.payload()).containsEntry("xpEarned", 75);
    }

    @Test
    void challengeCompleted_message_varies_with_result() {
        var catId = UUID.randomUUID();
        var opp = UUID.randomUUID();
        var chal = UUID.randomUUID();

        var won = new ChallengeCompleted(UUID.randomUUID(), Instant.now(),
                catId, chal, opp, ChallengeResult.WON, 100, 50);
        var lost = new ChallengeCompleted(UUID.randomUUID(), Instant.now(),
                catId, chal, opp, ChallengeResult.LOST, 100, 50);
        var draw = new ChallengeCompleted(UUID.randomUUID(), Instant.now(),
                catId, chal, opp, ChallengeResult.DRAW, 100, 50);

        assertThat(factory.from(won).orElseThrow().message()).containsIgnoringCase("ganado");
        assertThat(factory.from(lost).orElseThrow().message()).containsIgnoringCase("perdido");
        assertThat(factory.from(draw).orElseThrow().message()).containsIgnoringCase("empate");
    }

    @Test
    void badgeEarned_includes_name_and_rarity() {
        var catId = UUID.randomUUID();
        var event = new BadgeEarned(UUID.randomUUID(), Instant.now(),
                catId, UUID.randomUUID(), "Cazador Nocturno", "EPIC");

        var n = factory.from(event).orElseThrow();

        assertThat(n.type()).isEqualTo(NotificationType.BADGE_EARNED);
        assertThat(n.message()).contains("Cazador Nocturno").contains("EPIC");
    }

    // ── XpGained: no notifica desde from(), solo desde fromLevelUp() ──

    @Test
    void xpGained_returns_empty_from_factory() {
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), 30, "adventure", 30, 1);

        assertThat(factory.from(event)).isEmpty();
    }

    @Test
    void fromLevelUp_builds_level_up_notification() {
        var catId = UUID.randomUUID();
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                catId, 50, "challenge", 0, 5);

        var n = factory.fromLevelUp(event);

        assertThat(n.recipientCatId()).isEqualTo(catId);
        assertThat(n.type()).isEqualTo(NotificationType.LEVEL_UP);
        assertThat(n.message()).contains("5");
        assertThat(n.payload())
                .containsEntry("newLevel", 5)
                .containsEntry("source", "challenge");
    }

    // ── Eventos ignorados ──

    @Test
    void ignored_events_return_empty() {
        UUID catId = UUID.randomUUID();
        Instant now = Instant.now();

        CatastropheEvent[] ignored = {
                new CatCreated(UUID.randomUUID(), now, catId, UUID.randomUUID(), "Felix", "Siamese"),
                new CatUpdated(UUID.randomUUID(), now, catId, "name", "Felix", "Felix II"),
                new MeowPosted(UUID.randomUUID(), now, catId, UUID.randomUUID(), "PHOTO"),
                new AdventureStarted(UUID.randomUUID(), now, catId, UUID.randomUUID(), "easy"),
        };

        for (var event : ignored) {
            assertThat(factory.from(event))
                    .as("evento ignorado: %s", event.getClass().getSimpleName())
                    .isEmpty();
        }
    }
}
