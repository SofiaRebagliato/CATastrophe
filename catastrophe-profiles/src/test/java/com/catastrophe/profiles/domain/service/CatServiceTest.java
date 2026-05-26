package com.catastrophe.profiles.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.CatCreated;
import com.catastrophe.commons.event.CatastropheEvent.XpGained;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Cat;
import com.catastrophe.profiles.domain.port.in.CatUseCase.CreateCatCommand;
import com.catastrophe.profiles.domain.port.in.CatUseCase.UpdateCatCommand;
import com.catastrophe.profiles.domain.port.out.CatAvatarProvider;
import com.catastrophe.profiles.domain.port.out.CatRepository;
import com.catastrophe.profiles.domain.port.out.EventPublisher;
import com.catastrophe.profiles.domain.port.out.ProcessedXpEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de CatService.
 *
 * Validan la lógica de negocio de creación, actualización,
 * eliminación y refresco de avatar de gatos.
 */
@ExtendWith(MockitoExtension.class)
class CatServiceTest {

    @Mock private CatRepository catRepository;
    @Mock private CatAvatarProvider avatarProvider;
    @Mock private EventPublisher eventPublisher;
    @Mock private ProcessedXpEventRepository processedXpEvents;

    private CatService catService;

    private final UUID humanId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        catService = new CatService(catRepository, avatarProvider, eventPublisher, processedXpEvents);
    }

    @Nested
    @DisplayName("Creación de gatos")
    class Creation {

        @Test
        @DisplayName("Crear gato exitosamente obtiene avatar de TheCatAPI y publica evento")
        void createCatSuccessfully() {
            var command = new CreateCatCommand(humanId, "Bigotes", "persian", LocalDate.of(2024, 5, 1), "Un gato persa");

            when(catRepository.existsByHumanIdAndName(humanId, "Bigotes")).thenReturn(false);
            when(avatarProvider.fetchRandomAvatar("persian"))
                    .thenReturn(Optional.of("https://cdn2.thecatapi.com/images/abc.jpg"));
            when(catRepository.save(any(Cat.class))).thenAnswer(invocation -> {
                Cat c = invocation.getArgument(0);
                return new Cat(UUID.randomUUID(), c.humanId(), c.name(), c.breed(),
                        c.birthDate(), c.avatarUrl(), c.bio(), c.xp(), c.level(),
                        c.mood(), Instant.now(), null);
            });

            var result = catService.create(command);

            assertNotNull(result.id());
            assertEquals("Bigotes", result.name());
            assertEquals("https://cdn2.thecatapi.com/images/abc.jpg", result.avatarUrl());

            // Verificar que se publicó el evento CatCreated
            var eventCaptor = ArgumentCaptor.forClass(CatCreated.class);
            verify(eventPublisher).publish(eventCaptor.capture());
            assertEquals("Bigotes", eventCaptor.getValue().name());
        }

        @Test
        @DisplayName("Crear gato usa avatar por defecto si TheCatAPI falla")
        void createCatWithDefaultAvatar() {
            var command = new CreateCatCommand(humanId, "Luna", null, LocalDate.of(2025, 5, 1), null);

            when(catRepository.existsByHumanIdAndName(humanId, "Luna")).thenReturn(false);
            when(avatarProvider.fetchRandomAvatar(null)).thenReturn(Optional.empty());
            when(catRepository.save(any(Cat.class))).thenAnswer(invocation -> {
                Cat c = invocation.getArgument(0);
                return new Cat(UUID.randomUUID(), c.humanId(), c.name(), c.breed(),
                        c.birthDate(), c.avatarUrl(), c.bio(), c.xp(), c.level(),
                        c.mood(), Instant.now(), null);
            });

            var result = catService.create(command);

            assertEquals("https://cdn2.thecatapi.com/images/default.jpg", result.avatarUrl());
        }

        @Test
        @DisplayName("No se puede crear dos gatos con el mismo nombre para el mismo humano")
        void duplicateCatNameThrows() {
            var command = new CreateCatCommand(humanId, "Bigotes", null, LocalDate.of(2025, 5, 1), null);
            when(catRepository.existsByHumanIdAndName(humanId, "Bigotes")).thenReturn(true);

            var ex = assertThrows(BusinessRuleViolationException.class,
                    () -> catService.create(command));

            assertTrue(ex.getMessage().contains("Bigotes"));
            verify(catRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("findById devuelve el gato si existe")
        void findByIdReturns() {
            var catId = UUID.randomUUID();
            var cat = new Cat(catId, humanId, "Luna", null, LocalDate.of(2025, 5, 1), null, null,
                    0, 1, "curious", Instant.now(), null);
            when(catRepository.findById(catId)).thenReturn(Optional.of(cat));

            var result = catService.findById(catId);
            assertEquals("Luna", result.get().name());
        }

        @Test
        @DisplayName("findByHumanId devuelve todos los gatos del humano")
        void findByHumanIdReturnsList() {
            var cat1 = new Cat(UUID.randomUUID(), humanId, "Luna", null, LocalDate.of(2025, 5, 1), null, null,
                    0, 1, "curious", Instant.now(), null);
            var cat2 = new Cat(UUID.randomUUID(), humanId, "Bigotes", null, LocalDate.of(2024, 5, 1), null, null,
                    50, 1, "playful", Instant.now(), null);
            when(catRepository.findByHumanId(humanId)).thenReturn(List.of(cat1, cat2));

            var result = catService.findByHumanId(humanId);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("Actualización de gatos")
    class Update {

        private final UUID catId = UUID.randomUUID();
        private final Cat existingCat = new Cat(catId, humanId, "Luna", "siamese", LocalDate.of(2025, 5, 1),
                "https://cat.jpg", "Bio original", 50, 1, "curious", Instant.now(), null);

        @Test
        @DisplayName("Actualizar bio mantiene otros campos")
        void updateBioOnly() {
            when(catRepository.findById(catId)).thenReturn(Optional.of(existingCat));
            when(catRepository.save(any(Cat.class))).thenAnswer(i -> i.getArgument(0));

            var command = new UpdateCatCommand(null, null, null, "Nueva bio felina");
            var result = catService.update(catId, command);

            assertEquals("Nueva bio felina", result.bio());
            assertEquals("Luna", result.name());       // No cambió
            assertEquals("siamese", result.breed());    // No cambió
            assertEquals(LocalDate.of(2025, 5, 1), result.birthDate()); // No cambió
        }

        @Test
        @DisplayName("Actualizar gato inexistente lanza excepción")
        void updateNonExistentThrows() {
            var fakeId = UUID.randomUUID();
            when(catRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> catService.update(fakeId, new UpdateCatCommand("X", null, null, null)));
        }
    }

    @Nested
    @DisplayName("Refresco de avatar")
    class RefreshAvatar {

        @Test
        @DisplayName("Refrescar avatar obtiene nueva URL de TheCatAPI")
        void refreshAvatarGetsNewUrl() {
            var catId = UUID.randomUUID();
            var cat = new Cat(catId, humanId, "Luna", "persian", LocalDate.of(2025, 5, 1),
                    "https://old.jpg", null, 0, 1, "curious", Instant.now(), null);

            when(catRepository.findById(catId)).thenReturn(Optional.of(cat));
            when(avatarProvider.fetchRandomAvatar("persian"))
                    .thenReturn(Optional.of("https://new.jpg"));
            when(catRepository.save(any(Cat.class))).thenAnswer(i -> i.getArgument(0));

            var result = catService.refreshAvatar(catId);

            assertEquals("https://new.jpg", result.avatarUrl());
        }

        @Test
        @DisplayName("Si TheCatAPI falla, se mantiene el avatar actual")
        void refreshAvatarKeepsCurrent() {
            var catId = UUID.randomUUID();
            var cat = new Cat(catId, humanId, "Luna", "persian", LocalDate.of(2025, 5, 1),
                    "https://current.jpg", null, 0, 1, "curious", Instant.now(), null);

            when(catRepository.findById(catId)).thenReturn(Optional.of(cat));
            when(avatarProvider.fetchRandomAvatar("persian")).thenReturn(Optional.empty());
            when(catRepository.save(any(Cat.class))).thenAnswer(i -> i.getArgument(0));

            var result = catService.refreshAvatar(catId);

            assertEquals("https://current.jpg", result.avatarUrl());
        }
    }

    @Nested
    @DisplayName("Eliminación de gatos")
    class Deletion {

        @Test
        @DisplayName("Eliminar gato existente funciona")
        void deleteExistingCat() {
            var catId = UUID.randomUUID();
            var cat = new Cat(catId, humanId, "Luna", null, LocalDate.of(2025, 5, 1), null, null,
                    0, 1, "curious", Instant.now(), null);
            when(catRepository.findById(catId)).thenReturn(Optional.of(cat));

            assertDoesNotThrow(() -> catService.delete(catId));
            verify(catRepository).deleteById(catId);
        }

        @Test
        @DisplayName("Eliminar gato inexistente lanza excepción")
        void deleteNonExistentThrows() {
            var fakeId = UUID.randomUUID();
            when(catRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> catService.delete(fakeId));
        }
    }

    @Nested
    @DisplayName("Aplicación de XP")
    class XpApplication {

        private final UUID catId = UUID.randomUUID();
        private final UUID eventId = UUID.randomUUID();

        @Test
        @DisplayName("applyXpGain ignora eventos ya procesados (idempotencia)")
        void alreadyProcessedReturnsEmpty() {
            when(processedXpEvents.markProcessed(eventId, catId, 50, "adventure"))
                    .thenReturn(false);

            var result = catService.applyXpGain(eventId, catId, 50, "adventure");

            assertTrue(result.isEmpty());
            verify(catRepository, never()).findById(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("applyXpGain con gato inexistente marca como procesado y descarta")
        void unknownCatIsDiscarded() {
            when(processedXpEvents.markProcessed(eventId, catId, 50, "adventure"))
                    .thenReturn(true);
            when(catRepository.findById(catId)).thenReturn(Optional.empty());

            var result = catService.applyXpGain(eventId, catId, 50, "adventure");

            assertTrue(result.isEmpty());
            verify(catRepository, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("applyXpGain suma XP al gato y publica XpGained")
        void appliesAndPublishes() {
            var cat = new Cat(catId, humanId, "Luna", "siamese", LocalDate.of(2025, 5, 1), null, null,
                    50, 1, "curious", Instant.now(), null);
            when(processedXpEvents.markProcessed(eventId, catId, 80, "adventure"))
                    .thenReturn(true);
            when(catRepository.findById(catId)).thenReturn(Optional.of(cat));
            when(catRepository.save(any(Cat.class))).thenAnswer(i -> i.getArgument(0));

            var result = catService.applyXpGain(eventId, catId, 80, "adventure");

            assertTrue(result.isPresent());
            assertEquals(130, result.get().xp()); // 50 + 80
            // 130 < 4·100 → sigue en nivel 1 (umbral cuadrático: nivel 2 requiere 400)
            assertEquals(1, result.get().level());

            var captor = ArgumentCaptor.forClass(XpGained.class);
            verify(eventPublisher).publish(captor.capture());
            assertEquals(catId, captor.getValue().catId());
            assertEquals(80, captor.getValue().amount());
            assertEquals("adventure", captor.getValue().source());
            assertEquals(130, captor.getValue().newTotalXp());
            assertEquals(1, captor.getValue().newLevel());
        }

        @Test
        @DisplayName("applyXpGain detona level-up cuando se cruza el umbral cuadrático")
        void triggersLevelUp() {
            // Nivel 2 requiere 2²·100 = 400 XP totales
            var cat = new Cat(catId, humanId, "Luna", null, LocalDate.of(2025, 5, 1), null, null,
                    350, 1, "curious", Instant.now(), null);
            when(processedXpEvents.markProcessed(eventId, catId, 100, "challenge"))
                    .thenReturn(true);
            when(catRepository.findById(catId)).thenReturn(Optional.of(cat));
            when(catRepository.save(any(Cat.class))).thenAnswer(i -> i.getArgument(0));

            var result = catService.applyXpGain(eventId, catId, 100, "challenge");

            assertTrue(result.isPresent());
            assertEquals(450, result.get().xp());
            assertEquals(2, result.get().level()); // 450 ≥ 400 → sube a nivel 2
        }
    }
}
