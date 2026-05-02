package com.catastrophe.notifications.adapter.out.persistence;

import com.catastrophe.notifications.adapter.out.persistence.mapper.NotificationMapper;
import com.catastrophe.notifications.adapter.out.persistence.repository.JpaNotificationRepository;
import com.catastrophe.notifications.domain.model.Notification;
import com.catastrophe.notifications.domain.port.out.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia para notificaciones — implementa el puerto
 * {@code NotificationRepository} usando JPA.
 */
@Component
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final JpaNotificationRepository jpaRepository;
    private final NotificationMapper mapper;

    public NotificationPersistenceAdapter(JpaNotificationRepository jpaRepository,
                                          NotificationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = mapper.toEntity(notification);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpaRepository.existsByEventId(eventId);
    }

    @Override
    public List<Notification> findByRecipient(UUID recipientCatId, boolean unreadOnly, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var entities = unreadOnly
                ? jpaRepository.findByRecipientCatIdAndReadFalseOrderByCreatedAtDesc(recipientCatId, pageable)
                : jpaRepository.findByRecipientCatIdOrderByCreatedAtDesc(recipientCatId, pageable);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countUnread(UUID recipientCatId) {
        return jpaRepository.countByRecipientCatIdAndReadFalse(recipientCatId);
    }

    @Override
    public int markAllAsRead(UUID recipientCatId) {
        return jpaRepository.markAllAsRead(recipientCatId, Instant.now());
    }
}
