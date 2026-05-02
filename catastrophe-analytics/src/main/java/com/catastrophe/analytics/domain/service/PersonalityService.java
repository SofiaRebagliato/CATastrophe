package com.catastrophe.analytics.domain.service;

import com.catastrophe.commons.event.CatastropheEvent;
import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.event.CatastropheEvent.BadgeEarned;
import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.CatFollowed;
import com.catastrophe.commons.event.CatastropheEvent.CatUpdated;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.event.CatastropheEvent.PostCommented;
import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.port.in.PersonalityUseCase;
import com.catastrophe.analytics.domain.port.out.PersonalityRepository;
import com.catastrophe.analytics.domain.port.out.ProcessedPersonalityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de dominio — Personalidades felinas.
 * <p>
 * Orquesta: idempotencia → resolver receptor del impulso → calcular impulsos
 * vía {@link PersonalityCalculator} → suavizar y persistir cada trait afectado.
 */
@Service
@Transactional
public class PersonalityService implements PersonalityUseCase {

    private static final Logger log = LoggerFactory.getLogger(PersonalityService.class);

    private final PersonalityRepository repository;
    private final ProcessedPersonalityEventRepository processedEvents;
    private final PersonalityCalculator calculator;

    public PersonalityService(PersonalityRepository repository,
                              ProcessedPersonalityEventRepository processedEvents,
                              PersonalityCalculator calculator) {
        this.repository = repository;
        this.processedEvents = processedEvents;
        this.calculator = calculator;
    }

    @Override
    public void handleEvent(CatastropheEvent event) {
        var recipientCatId = recipientOf(event);
        if (recipientCatId == null) {
            // Evento que no afecta a personalidades (ej. CatCreated)
            return;
        }

        // Idempotencia: mismo evento no se aplica dos veces
        if (!processedEvents.markProcessed(
                event.eventId(),
                recipientCatId,
                event.getClass().getSimpleName())) {
            log.debug("Evento de personalidad {} ya procesado, ignorando", event.eventId());
            return;
        }

        var impulses = calculator.impulseFor(event);
        if (impulses.isEmpty()) {
            log.trace("Evento {} no produce impulsos de personalidad",
                    event.getClass().getSimpleName());
            return;
        }

        var personality = repository.findByCatId(recipientCatId);

        impulses.forEach((trait, impulse) -> {
            double newScore = calculator.smooth(personality.scoreOf(trait), impulse);
            repository.upsertScore(recipientCatId, trait, newScore);
            log.debug("Trait {} de gato {}: {} → {} (impulso {})",
                    trait, recipientCatId, personality.scoreOf(trait), newScore, impulse);
        });
    }

    /**
     * Determina a qué gato aplicar el impulso de un evento.
     * <p>
     * Para acciones recibidas (likes, comments en mi post, follow a mí) el
     * receptor es el dueño del recurso, no el actor. Para acciones propias
     * (publicar, completar aventura) el receptor es el propio actor.
     * <p>
     * Pattern matching exhaustivo: si commons añade un evento nuevo,
     * el compilador obliga a tomar una decisión aquí.
     *
     * @return el id del gato al que aplicar el impulso, o {@code null} si
     *         el evento no aplica a personalidad alguna
     */
    private UUID recipientOf(CatastropheEvent event) {
        return switch (event) {
            // Acciones del propio gato → impacto en su personalidad
            case MeowPosted e         -> e.catId();
            case PostCommented e      -> e.catId();
            case AdventureCompleted e -> e.catId();
            case ChallengeCompleted e -> e.catId();
            case BadgeEarned e        -> e.catId();
            case XpGained e           -> e.catId();

            // Acciones que recibe → impacto en quien recibe
            case PostLiked e   -> e.postOwnerId();
            case CatFollowed e -> e.followedCatId();

            // Eventos sin valor de personalidad
            case CatCreated _,
                 CatUpdated _,
                 AdventureStarted _ -> null;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Personality findByCatId(UUID catId) {
        return repository.findByCatId(catId);
    }
}
