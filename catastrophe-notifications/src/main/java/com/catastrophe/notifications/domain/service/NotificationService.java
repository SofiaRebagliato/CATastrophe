package com.catastrophe.notifications.domain.service;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.notifications.domain.model.Notification;
import com.catastrophe.notifications.domain.port.in.NotificationUseCase;
import com.catastrophe.notifications.domain.port.out.CatLevelTrackerRepository;
import com.catastrophe.notifications.domain.port.out.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de dominio — Orquesta la creación y consulta de notificaciones.
 * <p>
 * Es idempotente: si llega un evento ya procesado (mismo {@code eventId})
 * la operación no genera duplicados.
 */
@Service
@Transactional
public class NotificationService implements NotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final CatLevelTrackerRepository levelTracker;
    private final NotificationFactory factory;

    public NotificationService(NotificationRepository notificationRepository,
                               CatLevelTrackerRepository levelTracker,
                               NotificationFactory factory) {
        this.notificationRepository = notificationRepository;
        this.levelTracker = levelTracker;
        this.factory = factory;
    }

    @Override
    public Optional<Notification> handleEvent(CatastropheEvent event) {
        // Cortocircuito de idempotencia: si el evento ya generó notificación, salir.
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.debug("Evento {} ya procesado, ignorando (idempotencia)", event.eventId());
            return Optional.empty();
        }

        // Caso especial: XpGained requiere comparar contra el último nivel conocido.
        if (event instanceof XpGained xp) {
            return handleXpGained(xp);
        }

        // Resto de eventos: el factory decide
        var notification = factory.from(event);
        return notification.map(this::persistAndLog);
    }

    private Optional<Notification> handleXpGained(XpGained event) {
        var lastLevelOpt = levelTracker.findLastLevel(event.catId());

        // Siempre actualizamos el tracker con el nivel actual (incluso sin level-up).
        // Idempotente vía upsert.
        levelTracker.upsert(event.catId(), event.newLevel());

        if (lastLevelOpt.isEmpty()) {
            // Primera vez que vemos a este gato: no notificamos para evitar un
            // level-up espurio en el alta. A partir de ahora sí.
            log.debug("Primer XpGained registrado para gato {} (nivel {}), sin notificación",
                    event.catId(), event.newLevel());
            return Optional.empty();
        }

        int lastLevel = lastLevelOpt.get();
        if (event.newLevel() > lastLevel) {
            log.info("Level-up detectado para gato {}: {} → {}",
                    event.catId(), lastLevel, event.newLevel());
            var notification = factory.fromLevelUp(event);
            return Optional.of(persistAndLog(notification));
        }

        log.debug("XpGained sin level-up para gato {} (nivel {}), no se notifica",
                event.catId(), event.newLevel());
        return Optional.empty();
    }

    private Notification persistAndLog(Notification notification) {
        var saved = notificationRepository.save(notification);
        log.info("Notificación creada: type={}, recipient={}, id={}",
                saved.type(), saved.recipientCatId(), saved.id());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findFeed(UUID recipientCatId, boolean unreadOnly, int page, int size) {
        if (page < 0) {
            throw new BusinessRuleViolationException(
                    "INVALID_PAGE",
                    "El número de página no puede ser negativo");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessRuleViolationException(
                    "INVALID_SIZE",
                    "El tamaño de página debe estar entre 1 y 100");
        }
        return notificationRepository.findByRecipient(recipientCatId, unreadOnly, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID recipientCatId) {
        return notificationRepository.countUnread(recipientCatId);
    }

    @Override
    public Notification markAsRead(UUID notificationId, UUID recipientCatId) {
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        // Validación de propiedad: un gato no puede marcar como leídas las
        // notificaciones de otro. La cabecera X-Cat-Id viaja en el request.
        if (!notification.recipientCatId().equals(recipientCatId)) {
            throw new BusinessRuleViolationException(
                    "NOTIFICATION_OWNERSHIP",
                    "Esta notificación no es tuya. ¡Cada gato lee su propio buzón!");
        }

        return notificationRepository.save(notification.markAsRead());
    }

    @Override
    public int markAllAsRead(UUID recipientCatId) {
        int affected = notificationRepository.markAllAsRead(recipientCatId);
        log.info("Marcadas {} notificaciones como leídas para gato {}", affected, recipientCatId);
        return affected;
    }
}
