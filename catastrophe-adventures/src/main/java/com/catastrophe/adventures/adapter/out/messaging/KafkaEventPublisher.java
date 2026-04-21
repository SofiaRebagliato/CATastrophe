package com.catastrophe.adventures.adapter.out.messaging;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.KafkaTopics;
import com.catastrophe.adventures.domain.port.out.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida — Publica eventos de gamificación en Kafka.
 *
 * El servicio de aventuras emite eventos al topic GAMIFICATION_EVENTS.
 * Los consumidores de notificaciones y analytics reaccionan ante estos eventos.
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, CatastropheEvent> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, CatastropheEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(CatastropheEvent event) {
        var topic = KafkaTopics.GAMIFICATION_EVENTS;
        var key = event.catId().toString();

        log.debug("Publicando evento {} en topic '{}' con key '{}'",
                event.getClass().getSimpleName(), topic, key);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error publicando evento {} en Kafka: {}",
                                event.getClass().getSimpleName(), ex.getMessage(), ex);
                    } else {
                        log.debug("Evento {} publicado con offset {}",
                                event.getClass().getSimpleName(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
