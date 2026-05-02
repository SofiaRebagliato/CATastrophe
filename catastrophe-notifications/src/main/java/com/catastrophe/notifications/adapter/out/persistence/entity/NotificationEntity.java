package com.catastrophe.notifications.adapter.out.persistence.entity;

import com.catastrophe.notifications.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad JPA de notificaciones.
 * <p>
 * Mutable (a diferencia del record de dominio) porque JPA necesita un setter-friendly
 * lifecycle. La conversión a/desde el record la hace {@code NotificationMapper}.
 */
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "recipient_cat_id", nullable = false)
    private UUID recipientCatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /**
     * Payload contextual del evento. Se persiste como JSONB en PostgreSQL.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationEntity() {
        // Requerido por JPA
    }

    public NotificationEntity(UUID id,
                              UUID eventId,
                              UUID recipientCatId,
                              NotificationType type,
                              String message,
                              Map<String, Object> payload,
                              boolean read,
                              Instant createdAt,
                              Instant readAt) {
        this.id = id;
        this.eventId = eventId;
        this.recipientCatId = recipientCatId;
        this.type = type;
        this.message = message;
        this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getRecipientCatId() { return recipientCatId; }
    public void setRecipientCatId(UUID recipientCatId) { this.recipientCatId = recipientCatId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
