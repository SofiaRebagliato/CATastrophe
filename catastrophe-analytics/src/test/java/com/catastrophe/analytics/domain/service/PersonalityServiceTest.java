package com.catastrophe.analytics.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.AdventureCompleted;
import com.catastrophe.commons.event.CatastropheEvent.AdventureStarted;
import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.event.CatastropheEvent.PostLiked;
import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.model.Trait;
import com.catastrophe.analytics.domain.port.out.PersonalityRepository;
import com.catastrophe.analytics.domain.port.out.ProcessedPersonalityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalityServiceTest {

    private PersonalityRepository repository;
    private ProcessedPersonalityEventRepository processed;
    private PersonalityCalculator calculator;
    private PersonalityService service;

    @BeforeEach
    void setUp() {
        repository = mock(PersonalityRepository.class);
        processed = mock(ProcessedPersonalityEventRepository.class);
        calculator = new PersonalityCalculator();
        service = new PersonalityService(repository, processed, calculator);
    }

    @Test
    void handleEvent_for_meowPosted_upserts_social_score() {
        var catId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var event = new MeowPosted(eventId, Instant.now(), catId, UUID.randomUUID(), "PHOTO");

        when(processed.markProcessed(eq(eventId), eq(catId), anyString())).thenReturn(true);
        when(repository.findByCatId(catId)).thenReturn(Personality.empty(catId));

        service.handleEvent(event);

        verify(repository).upsertScore(eq(catId), eq(Trait.SOCIAL), anyDouble());
    }

    @Test
    void handleEvent_postLiked_upserts_for_post_owner_not_liker() {
        var liker = UUID.randomUUID();
        var owner = UUID.randomUUID();
        var event = new PostLiked(UUID.randomUUID(), Instant.now(),
                liker, UUID.randomUUID(), owner);

        when(processed.markProcessed(any(), eq(owner), anyString())).thenReturn(true);
        when(repository.findByCatId(owner)).thenReturn(Personality.empty(owner));

        service.handleEvent(event);

        verify(repository).upsertScore(eq(owner), eq(Trait.SOCIAL), anyDouble());
        verify(repository, never()).upsertScore(eq(liker), any(), anyDouble());
    }

    @Test
    void handleEvent_already_processed_returns_without_persisting() {
        var event = new MeowPosted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "PHOTO");
        when(processed.markProcessed(any(), any(), anyString())).thenReturn(false);

        service.handleEvent(event);

        verify(repository, never()).findByCatId(any());
        verify(repository, never()).upsertScore(any(), any(), anyDouble());
    }

    @Test
    void handleEvent_ignored_event_does_not_call_processed_tracker() {
        // CatCreated/AdventureStarted no afectan personalidad — el service ni
        // siquiera registra el evento en processed (porque el receptor es null).
        var event = new CatCreated(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "Felix", "Siamese");

        service.handleEvent(event);

        verify(processed, never()).markProcessed(any(), any(), anyString());
        verify(repository, never()).upsertScore(any(), any(), anyDouble());
    }

    @Test
    void handleEvent_adventureCompleted_upserts_multiple_traits() {
        var catId = UUID.randomUUID();
        var event = new AdventureCompleted(UUID.randomUUID(), Instant.now(),
                catId, UUID.randomUUID(), 100);

        when(processed.markProcessed(any(), eq(catId), anyString())).thenReturn(true);
        when(repository.findByCatId(catId)).thenReturn(Personality.empty(catId));

        service.handleEvent(event);

        // PLAYFUL y HUNTER (los traits afectados por AdventureCompleted)
        verify(repository).upsertScore(eq(catId), eq(Trait.PLAYFUL), anyDouble());
        verify(repository).upsertScore(eq(catId), eq(Trait.HUNTER), anyDouble());
    }

    @Test
    void handleEvent_does_nothing_when_event_has_no_impulses() {
        // AdventureStarted tiene catId no nulo pero impulseFor() devuelve mapa vacío.
        // Aún así el service NO debe pasar por idempotencia ni persistir nada.
        var event = new AdventureStarted(UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), UUID.randomUUID(), "easy");

        service.handleEvent(event);

        // Como recipientOf() devuelve null para AdventureStarted, no debe llamar
        // ni a processed ni a repository.
        verify(processed, never()).markProcessed(any(), any(), anyString());
        verify(repository, never()).upsertScore(any(), any(), anyDouble());
    }

    @Test
    void findByCatId_delegates_to_repository() {
        var catId = UUID.randomUUID();
        when(repository.findByCatId(catId)).thenReturn(Personality.empty(catId));

        var result = service.findByCatId(catId);

        verify(repository, atLeastOnce()).findByCatId(catId);
        org.assertj.core.api.Assertions.assertThat(result.catId()).isEqualTo(catId);
    }
}
