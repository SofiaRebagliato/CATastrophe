package com.catastrophe.commons.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test del sealed interface CatMood — demuestra exhaustividad del switch.
 */
class CatMoodTest {

    @Test
    @DisplayName("Pattern matching exhaustivo sobre CatMood sealed")
    void exhaustiveSwitchOnMoods() {
        CatMood mood = new CatMood.Mysterious();

        // Si añadimos un nuevo mood al sealed interface,
        // este switch NO compilará hasta que lo cubramos. ¡Seguridad en compilación!
        String emoji = switch (mood) {
            case CatMood.Curious c      -> "\uD83D\uDD0D";
            case CatMood.Playful p      -> "\uD83C\uDFBE";
            case CatMood.Sleepy s       -> "\uD83D\uDE34";
            case CatMood.Hungry h       -> "\uD83C\uDF56";
            case CatMood.Grumpy g       -> "\uD83D\uDE3E";
            case CatMood.Affectionate a -> "\uD83D\uDE3B";
            case CatMood.Mysterious m   -> "\uD83C\uDF19";
        };

        assertEquals("\uD83C\uDF19", emoji);
        assertEquals("Misterioso", mood.displayName());
    }

    @Test
    @DisplayName("Cada mood tiene nombre y descripción")
    void allMoodsHaveDisplayInfo() {
        CatMood[] allMoods = {
                new CatMood.Curious(),
                new CatMood.Playful(),
                new CatMood.Sleepy(),
                new CatMood.Hungry(),
                new CatMood.Grumpy(),
                new CatMood.Affectionate(),
                new CatMood.Mysterious()
        };

        for (var mood : allMoods) {
            assertNotNull(mood.displayName(), "displayName no debe ser null para " + mood);
            assertNotNull(mood.description(), "description no debe ser null para " + mood);
            assertFalse(mood.displayName().isBlank());
            assertFalse(mood.description().isBlank());
        }
    }
}
