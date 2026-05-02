package com.catastrophe.notifications.adapter.in.messaging;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.KafkaTopics;
import com.catastrophe.notifications.domain.port.in.NotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka — Escucha los topics de eventos sociales y de gamificación
 * y delega en el use case para materializar notificaciones.
 * <p>
 * No filtra ni decide aquí qué eventos son relevantes: esa decisión está
 * centralizada en {@code NotificationFactory} (pattern matching exhaustivo
 * sobre el sealed). Aquí solo desviamos al use case.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationUseCase useCase;

    public NotificationEventConsumer(NotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(
            topics = {KafkaTopics.SOCIAL_EVENTS, KafkaTopics.GAMIFICATION_EVENTS, KafkaTopics.PROFILE_EVENTS},
            groupId = "catastrophe-notifications"
    )
    public void onEvent(CatastropheEvent event) {
        log.debug("Evento recibido para notificaciones: {} (id={})",
                event.getClass().getSimpleName(), event.eventId());
        try {
            useCase.handleEvent(event);
        } catch (Exception ex) {
            // No relanzamos: queremos que el offset avance y el evento no quede
            // bloqueando la cola. Una notificación perdida es preferible a un
            // consumer atascado. El error queda en logs para análisis.
            log.error("Fallo procesando evento {} (id={}): {}",
                    event.getClass().getSimpleName(), event.eventId(), ex.getMessage(), ex);
        }
    }
}
