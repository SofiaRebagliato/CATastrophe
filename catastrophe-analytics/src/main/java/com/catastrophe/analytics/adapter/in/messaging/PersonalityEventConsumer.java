package com.catastrophe.analytics.adapter.in.messaging;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.KafkaTopics;
import com.catastrophe.analytics.domain.port.in.PersonalityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer Kafka — Recibe eventos de los tres topics y delega en el use case.
 * <p>
 * No filtra ni decide aquí: el {@code PersonalityCalculator} y el
 * {@code PersonalityService} centralizan la decisión de qué eventos afectan
 * a qué traits y de qué gato. Aquí solo desviamos.
 */
@Component
public class PersonalityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PersonalityEventConsumer.class);

    private final PersonalityUseCase useCase;

    public PersonalityEventConsumer(PersonalityUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(
            topics = {KafkaTopics.SOCIAL_EVENTS,
                      KafkaTopics.GAMIFICATION_EVENTS,
                      KafkaTopics.PROFILE_EVENTS},
            groupId = "catastrophe-analytics-personality"
    )
    public void onEvent(CatastropheEvent event) {
        log.debug("Personality consumer recibió {} (id={})",
                event.getClass().getSimpleName(), event.eventId());
        try {
            useCase.handleEvent(event);
        } catch (Exception ex) {
            log.error("Fallo procesando evento {} (id={}) en analytics: {}",
                    event.getClass().getSimpleName(), event.eventId(), ex.getMessage(), ex);
        }
    }
}
