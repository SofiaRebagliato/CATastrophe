package com.catastrophe.adventures.domain.model;

import com.catastrophe.commons.model.AdventureStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de los modelos CatAdventure y Adventure — transiciones de estado
 * y ventana de disponibilidad temporal.
 */
class CatAdventureTest {

    @Nested
    @DisplayName("Transiciones de CatAdventure")
    class Transitions {

        @Test
        @DisplayName("start arranca IN_PROGRESS al 0% sin fecha de fin")
        void startsInProgress() {
            var ca = CatAdventure.start(UUID.randomUUID(), UUID.randomUUID());

            assertThat(ca.status()).isEqualTo(AdventureStatus.IN_PROGRESS);
            assertThat(ca.progressPct()).isZero();
            assertThat(ca.completedAt()).isNull();
        }

        @Test
        @DisplayName("Progreso parcial mantiene IN_PROGRESS")
        void partialProgressStaysInProgress() {
            var ca = CatAdventure.start(UUID.randomUUID(), UUID.randomUUID()).updateProgress(60);

            assertThat(ca.status()).isEqualTo(AdventureStatus.IN_PROGRESS);
            assertThat(ca.progressPct()).isEqualTo(60);
        }

        @Test
        @DisplayName("Progreso >= 100 completa y fija completedAt")
        void fullProgressCompletes() {
            var ca = CatAdventure.start(UUID.randomUUID(), UUID.randomUUID()).updateProgress(100);

            assertThat(ca.isCompleted()).isTrue();
            assertThat(ca.progressPct()).isEqualTo(100);
            assertThat(ca.completedAt()).isNotNull();
        }

        @Test
        @DisplayName("abandon marca ABANDONED conservando el progreso")
        void abandonKeepsProgress() {
            var ca = CatAdventure.start(UUID.randomUUID(), UUID.randomUUID()).updateProgress(40).abandon();

            assertThat(ca.status()).isEqualTo(AdventureStatus.ABANDONED);
            assertThat(ca.progressPct()).isEqualTo(40);
        }
    }

    @Nested
    @DisplayName("Ventana de disponibilidad de Adventure")
    class Availability {

        private final Instant now = Instant.now();

        private Adventure withWindow(Instant from, Instant until) {
            return new Adventure(UUID.randomUUID(), "t", "d", Adventure.DIFFICULTY_EASY,
                    10, Adventure.TYPE_DAILY, false, from, until);
        }

        @Test
        @DisplayName("Sin l\u00edmites siempre est\u00e1 disponible")
        void noBoundsAlwaysAvailable() {
            assertThat(withWindow(null, null).isAvailableAt(now)).isTrue();
        }

        @Test
        @DisplayName("Antes de availableFrom no est\u00e1 disponible")
        void beforeFromUnavailable() {
            assertThat(withWindow(now.plus(1, ChronoUnit.DAYS), null).isAvailableAt(now)).isFalse();
        }

        @Test
        @DisplayName("Despu\u00e9s de availableUntil no est\u00e1 disponible")
        void afterUntilUnavailable() {
            assertThat(withWindow(null, now.minus(1, ChronoUnit.DAYS)).isAvailableAt(now)).isFalse();
        }

        @Test
        @DisplayName("Dentro de la ventana est\u00e1 disponible")
        void insideWindowAvailable() {
            var adv = withWindow(now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));
            assertThat(adv.isAvailableAt(now)).isTrue();
        }
    }
}
