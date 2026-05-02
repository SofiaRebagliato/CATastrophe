package com.catastrophe.notifications.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void newOne_creates_unread_notification_without_id() {
        var eventId = UUID.randomUUID();
        var catId = UUID.randomUUID();

        var n = Notification.newOne(
                eventId, catId,
                NotificationType.POST_LIKED,
                "Te han dado like",
                Map.of("postId", "abc")
        );

        assertThat(n.id()).isNull();
        assertThat(n.eventId()).isEqualTo(eventId);
        assertThat(n.recipientCatId()).isEqualTo(catId);
        assertThat(n.read()).isFalse();
        assertThat(n.readAt()).isNull();
        assertThat(n.createdAt()).isNotNull();
        assertThat(n.payload()).containsEntry("postId", "abc");
    }

    @Test
    void markAsRead_returns_new_instance_with_read_true_and_readAt() {
        var n = Notification.newOne(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.BADGE_EARNED,
                "Insignia obtenida",
                Map.of()
        );

        var read = n.markAsRead();

        assertThat(read).isNotSameAs(n);
        assertThat(read.read()).isTrue();
        assertThat(read.readAt()).isNotNull();
        assertThat(n.read()).isFalse(); // inmutabilidad
    }

    @Test
    void markAsRead_is_idempotent_when_already_read() {
        var n = Notification.newOne(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.LEVEL_UP,
                "Subiste de nivel",
                Map.of()
        ).markAsRead();

        var twice = n.markAsRead();

        assertThat(twice).isSameAs(n);
    }

    @Test
    void rejects_blank_message() {
        assertThatThrownBy(() -> Notification.newOne(
                UUID.randomUUID(), UUID.randomUUID(),
                NotificationType.POST_LIKED,
                "   ",
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_required_fields() {
        assertThatThrownBy(() -> Notification.newOne(
                null, UUID.randomUUID(),
                NotificationType.POST_LIKED,
                "msg",
                Map.of()
        )).isInstanceOf(NullPointerException.class);
    }
}
