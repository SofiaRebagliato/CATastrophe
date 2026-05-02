package com.catastrophe.notifications.domain.port.in;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.notifications.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de notificaciones.
 * <p>
 * Implementado por {@code NotificationService}. Lo invocan tanto el consumer
 * Kafka (para crear notificaciones a partir de eventos) como el controller REST
 * (para consultarlas y marcarlas como leídas).
 */
public interface NotificationUseCase {

    /**
     * Procesa un evento del bus y, si es relevante, materializa la notificación
     * correspondiente. Es idempotente: si el {@code eventId} ya generó una
     * notificación previamente, devuelve {@code Optional.empty()}.
     */
    Optional<Notification> handleEvent(CatastropheEvent event);

    List<Notification> findFeed(UUID recipientCatId, boolean unreadOnly, int page, int size);

    long countUnread(UUID recipientCatId);

    Notification markAsRead(UUID notificationId, UUID recipientCatId);

    int markAllAsRead(UUID recipientCatId);
}
