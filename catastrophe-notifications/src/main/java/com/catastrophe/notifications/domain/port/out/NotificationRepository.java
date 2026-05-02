package com.catastrophe.notifications.domain.port.out;

import com.catastrophe.notifications.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de notificaciones.
 * <p>
 * El adaptador concreto vive en {@code adapter.out.persistence}.
 */
public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /**
     * Indica si ya existe una notificación generada por el evento dado.
     * Clave de idempotencia: cada {@code CatastropheEvent.eventId()} genera
     * como mucho una notificación.
     */
    boolean existsByEventId(UUID eventId);

    /**
     * Listado paginado para el feed del gato, ordenado por fecha descendente.
     *
     * @param recipientCatId destinatario
     * @param unreadOnly     si {@code true} filtra solo no-leídas
     * @param page           número de página, 0-indexed
     * @param size           tamaño de página
     */
    List<Notification> findByRecipient(UUID recipientCatId, boolean unreadOnly, int page, int size);

    /**
     * Cuenta de notificaciones no-leídas para el gato.
     */
    long countUnread(UUID recipientCatId);

    /**
     * Marca todas las notificaciones no-leídas del gato como leídas en una sola operación.
     *
     * @return número de notificaciones afectadas
     */
    int markAllAsRead(UUID recipientCatId);
}
