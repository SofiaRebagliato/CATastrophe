package com.catastrophe.notifications.adapter.out.persistence.mapper;

import com.catastrophe.notifications.adapter.out.persistence.entity.NotificationEntity;
import com.catastrophe.notifications.domain.model.Notification;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper bidireccional entre el record de dominio y la entity JPA.
 */
@Component
public class NotificationMapper {

    public NotificationEntity toEntity(Notification n) {
        return new NotificationEntity(
                n.id() != null ? n.id() : UUID.randomUUID(),
                n.eventId(),
                n.recipientCatId(),
                n.type(),
                n.message(),
                new HashMap<>(n.payload()),
                n.read(),
                n.createdAt(),
                n.readAt()
        );
    }

    public Notification toDomain(NotificationEntity e) {
        return new Notification(
                e.getId(),
                e.getEventId(),
                e.getRecipientCatId(),
                e.getType(),
                e.getMessage(),
                e.getPayload() != null ? Map.copyOf(e.getPayload()) : Map.of(),
                e.isRead(),
                e.getCreatedAt(),
                e.getReadAt()
        );
    }
}
