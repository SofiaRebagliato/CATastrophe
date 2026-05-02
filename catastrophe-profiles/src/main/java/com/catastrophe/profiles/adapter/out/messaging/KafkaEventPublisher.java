package com.catastrophe.profiles.adapter.out.messaging;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.event.KafkaTopics;
import com.catastrophe.profiles.domain.port.out.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida — Publica eventos de dominio de perfiles en Kafka.
 * <p>
 * El topic se elige por <em>dominio del evento</em>, no por servicio emisor:
 * {@code XpGained} viaja por {@code GAMIFICATION_EVENTS} aunque lo emita
 * profiles, para que el consumer de rankings de adventures lo reciba sin
 * tener que escuchar también el topic de perfiles. Los eventos puramente
 * de identidad ({@code CatCreated}, {@code CatUpdated}) van a {@code PROFILE_EVENTS}.
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
        var topic = topicFor(event);
        var key = event.catId().toString();

        log.debug("Publicando evento {} en topic '{}' con key '{}'",
                event.getClass().getSimpleName(), topic, key);

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error publicando evento en Kafka: {}", ex.getMessage(), ex);
                    } else {
                        log.debug("Evento {} publicado con offset {}",
                                event.getClass().getSimpleName(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Decide el topic destino según el tipo de evento. Pattern matching sobre
     * el sealed interface, así si añades un evento nuevo el compilador te avisa.
     */
    private String topicFor(CatastropheEvent event) {
        return switch (event) {
            case XpGained _ -> KafkaTopics.GAMIFICATION_EVENTS;
            default         -> KafkaTopics.PROFILE_EVENTS;
        };
    }
}
