package com.catastrophe.commons.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test que demuestra el uso de pattern matching con sealed interfaces.
 * 
 * Este es uno de los patrones clave de Java 21 que usaremos en los
 * consumidores de Kafka para procesar eventos de dominio.
 */
class CatastropheEventTest {

    @Test
    @DisplayName("Pattern matching exhaustivo sobre eventos sealed")
    void patternMatchingOnSealedEvents() {
        CatastropheEvent event = new CatastropheEvent.MeowPosted(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "meow"
        );

        // El compilador garantiza que cubrimos todos los casos
        String result = switch (event) {
            case CatastropheEvent.CatCreated e      -> "Nuevo gato: " + e.name();
            case CatastropheEvent.CatUpdated e       -> "Gato actualizado: " + e.field();
            case CatastropheEvent.MeowPosted e       -> "Nuevo meow tipo: " + e.postType();
            case CatastropheEvent.PostLiked e        -> "Like en post: " + e.postId();
            case CatastropheEvent.PostCommented e    -> "Comentario en: " + e.postId();
            case CatastropheEvent.CatFollowed e      -> "Siguiendo a: " + e.followedCatId();
            case CatastropheEvent.AdventureStarted e -> "Aventura iniciada: " + e.difficulty();
            case CatastropheEvent.AdventureCompleted e -> "Aventura completada: +" + e.xpEarned() + "XP";
            case CatastropheEvent.ChallengeCompleted e -> "Reto: " + e.result();
            case CatastropheEvent.BadgeEarned e      -> "Badge: " + e.badgeName() + " (" + e.rarity() + ")";
            case CatastropheEvent.XpGained e         -> "+" + e.amount() + "XP → nivel " + e.newLevel();
        };

        assertEquals("Nuevo meow tipo: meow", result);
    }

    @Test
    @DisplayName("Los eventos son records inmutables con todos los datos necesarios")
    void eventsAreImmutableRecords() {
        var catId = UUID.randomUUID();
        var event = new CatastropheEvent.BadgeEarned(
                UUID.randomUUID(),
                Instant.now(),
                catId,
                UUID.randomUUID(),
                "Cazador Legendario",
                "legendary"
        );

        // Records: igualdad estructural, toString(), etc. gratis
        assertEquals(catId, event.catId());
        assertEquals("legendary", event.rarity());
        assertNotNull(event.toString()); // Generado automáticamente
    }

    @Test
    @DisplayName("Se puede extraer datos con pattern matching y guards")
    void patternMatchingWithGuards() {
        CatastropheEvent event = new CatastropheEvent.XpGained(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                500,
                "adventure_completed",
                1500,
                5
        );

        // Pattern matching con guard clauses (Java 21)
        String notification = switch (event) {
            case CatastropheEvent.XpGained xp when xp.newLevel() >= 10 ->
                    "¡Nivel legendario alcanzado!";
            case CatastropheEvent.XpGained xp when xp.amount() >= 100 ->
                    "¡Gran recompensa de %d XP!".formatted(xp.amount());
            case CatastropheEvent.XpGained xp ->
                    "+%d XP".formatted(xp.amount());
            default -> "Evento procesado";
        };

        assertEquals("¡Gran recompensa de 500 XP!", notification);
    }
}
