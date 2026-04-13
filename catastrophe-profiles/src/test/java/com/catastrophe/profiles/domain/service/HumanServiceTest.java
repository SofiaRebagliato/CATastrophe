package com.catastrophe.profiles.domain.service;

import com.catastrophe.commons.exception.CatastropheExceptions.DuplicateResourceException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Human;
import com.catastrophe.profiles.domain.port.in.HumanUseCase.RegisterHumanCommand;
import com.catastrophe.profiles.domain.port.in.HumanUseCase.UpdateHumanCommand;
import com.catastrophe.profiles.domain.port.out.HumanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de HumanService.
 *
 * Usan mocks puros — sin Spring, sin base de datos, sin Docker.
 * Validan la lógica de negocio del registro, actualización y desactivación.
 */
@ExtendWith(MockitoExtension.class)
class HumanServiceTest {

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private HumanService humanService;

    @BeforeEach
    void setUp() {
        humanService = new HumanService(humanRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("Registro de humanos")
    class Registration {

        @Test
        @DisplayName("Registro exitoso crea un humano con contraseña hasheada")
        void successfulRegistration() {
            var command = new RegisterHumanCommand(
                    "gatero99", "gatero@mail.com", "MiPassword123", "El Gatero"
            );

            when(humanRepository.existsByUsername("gatero99")).thenReturn(false);
            when(humanRepository.existsByEmail("gatero@mail.com")).thenReturn(false);
            when(passwordEncoder.encode("MiPassword123")).thenReturn("$2a$hashed");
            when(humanRepository.save(any(Human.class))).thenAnswer(invocation -> {
                Human h = invocation.getArgument(0);
                return new Human(UUID.randomUUID(), h.username(), h.email(),
                        h.passwordHash(), h.displayName(), Instant.now(), null, true);
            });

            var result = humanService.register(command);

            assertNotNull(result.id());
            assertEquals("gatero99", result.username());
            assertEquals("gatero@mail.com", result.email());
            assertEquals("El Gatero", result.displayName());
            assertTrue(result.active());

            // Verificar que la contraseña se hasheó
            var captor = ArgumentCaptor.forClass(Human.class);
            verify(humanRepository).save(captor.capture());
            assertEquals("$2a$hashed", captor.getValue().passwordHash());
        }

        @Test
        @DisplayName("Registro falla si el username ya existe")
        void duplicateUsernameThrows() {
            var command = new RegisterHumanCommand(
                    "existente", "nuevo@mail.com", "Pass1234", "Display"
            );
            when(humanRepository.existsByUsername("existente")).thenReturn(true);

            var ex = assertThrows(DuplicateResourceException.class,
                    () -> humanService.register(command));

            assertTrue(ex.getMessage().contains("existente"));
            verify(humanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Registro falla si el email ya existe")
        void duplicateEmailThrows() {
            var command = new RegisterHumanCommand(
                    "nuevo", "existente@mail.com", "Pass1234", "Display"
            );
            when(humanRepository.existsByUsername("nuevo")).thenReturn(false);
            when(humanRepository.existsByEmail("existente@mail.com")).thenReturn(true);

            var ex = assertThrows(DuplicateResourceException.class,
                    () -> humanService.register(command));

            assertTrue(ex.getMessage().contains("existente@mail.com"));
            verify(humanRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Actualización de humanos")
    class Update {

        private final UUID humanId = UUID.randomUUID();
        private final Human existingHuman = new Human(
                humanId, "gatero99", "gatero@mail.com", "$2a$hashed",
                "El Gatero", Instant.now(), null, true
        );

        @Test
        @DisplayName("Actualizar displayName mantiene otros campos")
        void updateDisplayName() {
            when(humanRepository.findById(humanId)).thenReturn(Optional.of(existingHuman));
            when(humanRepository.save(any(Human.class))).thenAnswer(i -> i.getArgument(0));

            var command = new UpdateHumanCommand("Nuevo Nombre", null);
            var result = humanService.update(humanId, command);

            assertEquals("Nuevo Nombre", result.displayName());
            assertEquals("gatero@mail.com", result.email()); // No cambió
        }

        @Test
        @DisplayName("Actualizar un humano inexistente lanza excepción")
        void updateNonExistentThrows() {
            var fakeId = UUID.randomUUID();
            when(humanRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> humanService.update(fakeId, new UpdateHumanCommand("X", null)));
        }
    }

    @Nested
    @DisplayName("Último login")
    class LastLogin {

        @Test
        @DisplayName("updateLastLogin actualiza el timestamp")
        void updatesLastLogin() {
            var humanId = UUID.randomUUID();
            var human = new Human(humanId, "user", "u@m.com", "hash",
                    "Display", Instant.now(), null, true);

            when(humanRepository.findById(humanId)).thenReturn(Optional.of(human));
            when(humanRepository.save(any(Human.class))).thenAnswer(i -> i.getArgument(0));

            var result = humanService.updateLastLogin(humanId);

            assertNotNull(result.lastLogin());
        }
    }

    @Nested
    @DisplayName("Desactivación")
    class Deactivation {

        @Test
        @DisplayName("Desactivar un humano pone active a false")
        void deactivateSetsInactive() {
            var humanId = UUID.randomUUID();
            var human = new Human(humanId, "user", "u@m.com", "hash",
                    "Display", Instant.now(), null, true);

            when(humanRepository.findById(humanId)).thenReturn(Optional.of(human));
            when(humanRepository.save(any(Human.class))).thenAnswer(i -> i.getArgument(0));

            humanService.deactivate(humanId);

            var captor = ArgumentCaptor.forClass(Human.class);
            verify(humanRepository).save(captor.capture());
            assertFalse(captor.getValue().active());
        }
    }
}
