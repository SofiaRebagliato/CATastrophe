package com.catastrophe.profiles.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del modelo de dominio Human.
 *
 * Tests puros de lógica — sin Spring, sin DB, sin Docker.
 */
class HumanTest {

    @Nested
    @DisplayName("Creación de humanos")
    class Creation {

        @Test
        @DisplayName("Un humano nuevo se crea activo y sin id")
        void newHumanIsActiveWithNoId() {
            var human = Human.create("gatero99", "gatero@mail.com", "hashedPwd", "El Gatero");

            assertNull(human.id(), "El id lo genera la base de datos");
            assertEquals("gatero99", human.username());
            assertEquals("gatero@mail.com", human.email());
            assertEquals("hashedPwd", human.passwordHash());
            assertEquals("El Gatero", human.displayName());
            assertTrue(human.active());
            assertNull(human.createdAt(), "El timestamp lo genera la base de datos");
            assertNull(human.lastLogin());
        }
    }

    @Nested
    @DisplayName("Inmutabilidad")
    class Immutability {

        private final Human original = new Human(
                UUID.randomUUID(), "user", "u@m.com", "hash",
                "Display", Instant.now(), null, true
        );

        @Test
        @DisplayName("withLastLogin devuelve una nueva instancia")
        void withLastLoginIsImmutable() {
            var loginTime = Instant.now();
            var updated = original.withLastLogin(loginTime);

            assertNull(original.lastLogin(), "El original no debe cambiar");
            assertEquals(loginTime, updated.lastLogin());
            assertNotSame(original, updated);
        }

        @Test
        @DisplayName("deactivate devuelve una nueva instancia inactiva")
        void deactivateIsImmutable() {
            var deactivated = original.deactivate();

            assertTrue(original.active(), "El original no debe cambiar");
            assertFalse(deactivated.active());
            assertNotSame(original, deactivated);
        }

        @Test
        @DisplayName("deactivate mantiene todos los demás campos")
        void deactivatePreservesFields() {
            var deactivated = original.deactivate();

            assertEquals(original.id(), deactivated.id());
            assertEquals(original.username(), deactivated.username());
            assertEquals(original.email(), deactivated.email());
            assertEquals(original.passwordHash(), deactivated.passwordHash());
            assertEquals(original.displayName(), deactivated.displayName());
            assertEquals(original.createdAt(), deactivated.createdAt());
        }
    }
}
