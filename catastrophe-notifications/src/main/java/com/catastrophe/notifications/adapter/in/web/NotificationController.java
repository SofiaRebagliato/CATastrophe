package com.catastrophe.notifications.adapter.in.web;

import com.catastrophe.notifications.domain.model.Notification;
import com.catastrophe.notifications.domain.model.NotificationType;
import com.catastrophe.notifications.domain.port.in.NotificationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Notificaciones.
 * <p>
 * Convención del proyecto: el header {@code X-Cat-Id} identifica al gato actor
 * de la petición (lo inyecta el gateway una vez resuelta la sesión).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationUseCase useCase;

    public NotificationController(NotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestHeader("X-Cat-Id") UUID catId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var feed = useCase.findFeed(catId, unreadOnly, page, size).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/count")
    public ResponseEntity<UnreadCountResponse> countUnread(@RequestHeader("X-Cat-Id") UUID catId) {
        long count = useCase.countUnread(catId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var updated = useCase.markAsRead(id, catId);
        return ResponseEntity.ok(NotificationResponse.from(updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllResponse> markAllAsRead(@RequestHeader("X-Cat-Id") UUID catId) {
        int affected = useCase.markAllAsRead(catId);
        return ResponseEntity.ok(new MarkAllResponse(affected));
    }

    // ── DTOs de respuesta ──

    public record NotificationResponse(
            UUID id,
            NotificationType type,
            String message,
            Map<String, Object> payload,
            boolean read,
            Instant createdAt,
            Instant readAt
    ) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.id(), n.type(), n.message(), n.payload(),
                    n.read(), n.createdAt(), n.readAt()
            );
        }
    }

    public record UnreadCountResponse(long unreadCount) {}

    public record MarkAllResponse(int markedAsRead) {}
}
