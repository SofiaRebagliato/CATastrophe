package com.catastrophe.notifications.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.notifications.domain.model.Notification;
import com.catastrophe.notifications.domain.model.NotificationType;
import com.catastrophe.notifications.domain.port.out.CatLevelTrackerRepository;
import com.catastrophe.notifications.domain.port.out.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository repository;
    private CatLevelTrackerRepository tracker;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        tracker = mock(CatLevelTrackerRepository.class);
        var factory = new NotificationFactory();
        service = new NotificationService(repository, tracker, factory);

        // Por defecto, save devuelve la misma notificación con un id asignado
        when(repository.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return new Notification(
                    n.id() != null ? n.id() : UUID.randomUUID(),
                    n.eventId(), n.recipientCatId(), n.type(),
                    n.message(), n.payload(), n.read(),
                    n.createdAt(), n.readAt());
        });
    }

    // ── handleEvent ──

    @Test
    void handleEvent_persists_notification_for_relevant_event() {
        var event = new PostLiked(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        var result = service.handleEvent(event);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(NotificationType.POST_LIKED);
        verify(repository, times(1)).save(any());
    }

    @Test
    void handleEvent_is_idempotent_when_event_already_processed() {
        var event = new PostLiked(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(repository.existsByEventId(event.eventId())).thenReturn(true);

        var result = service.handleEvent(event);

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void handleEvent_returns_empty_for_ignored_event_types() {
        // AdventureStarted no genera notificación; comprobamos que no falla y no persiste.
        var event = new com.catastrophe.commons.event.CatastropheEvent.AdventureStarted(
                UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "easy");
        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        var result = service.handleEvent(event);

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void handleEvent_persists_adventure_completed() {
        var event = new AdventureCompleted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), 100);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        var result = service.handleEvent(event);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(NotificationType.ADVENTURE_COMPLETED);
    }

    // ── XpGained / level-up ──

    @Test
    void xpGained_with_level_up_creates_notification_and_updates_tracker() {
        var catId = UUID.randomUUID();
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                catId, 100, "adventure", 0, 3);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);
        when(tracker.findLastLevel(catId)).thenReturn(Optional.of(2));

        var result = service.handleEvent(event);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(NotificationType.LEVEL_UP);
        verify(tracker).upsert(catId, 3);
    }

    @Test
    void xpGained_without_level_up_does_not_notify_but_updates_tracker() {
        var catId = UUID.randomUUID();
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                catId, 30, "adventure", 30, 2);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);
        when(tracker.findLastLevel(catId)).thenReturn(Optional.of(2));

        var result = service.handleEvent(event);

        assertThat(result).isEmpty();
        verify(tracker).upsert(catId, 2);
        verify(repository, never()).save(any());
    }

    @Test
    void xpGained_first_time_seen_does_not_notify_but_records_level() {
        // Primera vez que vemos al gato: registramos su nivel pero no notificamos
        // (evitamos un level-up espurio en el alta del gato).
        var catId = UUID.randomUUID();
        var event = new XpGained(UUID.randomUUID(), Instant.now(),
                catId, 50, "challenge", 50, 1);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);
        when(tracker.findLastLevel(catId)).thenReturn(Optional.empty());

        var result = service.handleEvent(event);

        assertThat(result).isEmpty();
        verify(tracker).upsert(catId, 1);
        verify(repository, never()).save(any());
    }

    // ── Read / mark ──

    @Test
    void markAsRead_persists_change_and_returns_updated() {
        var catId = UUID.randomUUID();
        var notifId = UUID.randomUUID();
        var unread = new Notification(notifId, UUID.randomUUID(), catId,
                NotificationType.POST_LIKED, "msg", Map.of(),
                false, Instant.now(), null);
        when(repository.findById(notifId)).thenReturn(Optional.of(unread));

        var result = service.markAsRead(notifId, catId);

        assertThat(result.read()).isTrue();
        assertThat(result.readAt()).isNotNull();
    }

    @Test
    void markAsRead_throws_when_notification_belongs_to_another_cat() {
        var owner = UUID.randomUUID();
        var intruder = UUID.randomUUID();
        var notifId = UUID.randomUUID();
        var unread = new Notification(notifId, UUID.randomUUID(), owner,
                NotificationType.POST_LIKED, "msg", Map.of(),
                false, Instant.now(), null);
        when(repository.findById(notifId)).thenReturn(Optional.of(unread));

        assertThatThrownBy(() -> service.markAsRead(notifId, intruder))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no es tuya");
    }

    @Test
    void markAsRead_throws_when_notification_does_not_exist() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(id, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_delegates_and_returns_count() {
        var catId = UUID.randomUUID();
        when(repository.markAllAsRead(catId)).thenReturn(7);

        assertThat(service.markAllAsRead(catId)).isEqualTo(7);
    }

    // ── findFeed validations ──

    @Test
    void findFeed_rejects_negative_page() {
        assertThatThrownBy(() -> service.findFeed(UUID.randomUUID(), false, -1, 10))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void findFeed_rejects_invalid_size() {
        assertThatThrownBy(() -> service.findFeed(UUID.randomUUID(), false, 0, 0))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> service.findFeed(UUID.randomUUID(), false, 0, 101))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void findFeed_delegates_to_repository() {
        var catId = UUID.randomUUID();
        when(repository.findByRecipient(eq(catId), eq(true), eq(0), anyInt()))
                .thenReturn(List.of());

        var result = service.findFeed(catId, true, 0, 20);

        assertThat(result).isEmpty();
        verify(repository).findByRecipient(catId, true, 0, 20);
    }
}
