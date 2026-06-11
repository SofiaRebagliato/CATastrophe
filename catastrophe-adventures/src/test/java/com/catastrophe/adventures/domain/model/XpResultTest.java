package com.catastrophe.adventures.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del modelo XpResult — curva cuadr\u00e1tica de niveles (N\u00b2 \u00d7 100).
 */
class XpResultTest {

    @Test
    @DisplayName("El umbral de cada nivel sigue N\u00b2 \u00d7 100")
    void thresholdsFollowQuadraticCurve() {
        assertThat(XpResult.xpForLevel(1)).isEqualTo(100);
        assertThat(XpResult.xpForLevel(2)).isEqualTo(400);
        assertThat(XpResult.xpForLevel(3)).isEqualTo(900);
        assertThat(XpResult.xpForLevel(5)).isEqualTo(2500);
    }

    @Test
    @DisplayName("Poco XP no sube de nivel")
    void smallGainDoesNotLevelUp() {
        var result = XpResult.calculate(0, 1, 50);

        assertThat(result.totalXp()).isEqualTo(50);
        assertThat(result.level()).isEqualTo(1);
        assertThat(result.leveledUp()).isFalse();
    }

    @Test
    @DisplayName("Alcanzar el umbral sube exactamente un nivel")
    void reachingThresholdLevelsUp() {
        var result = XpResult.calculate(0, 1, 400);

        assertThat(result.level()).isEqualTo(2);
        assertThat(result.leveledUp()).isTrue();
    }

    @Test
    @DisplayName("Una gran ganancia de XP puede subir varios niveles de golpe")
    void bigGainSkipsLevels() {
        var result = XpResult.calculate(0, 1, 2600);

        assertThat(result.level()).isGreaterThanOrEqualTo(5);
        assertThat(result.leveledUp()).isTrue();
    }

    @Test
    @DisplayName("El XP previo se acumula al calcular el nuevo total")
    void accumulatesPreviousXp() {
        var result = XpResult.calculate(350, 1, 100);

        assertThat(result.totalXp()).isEqualTo(450);
        assertThat(result.level()).isEqualTo(2);
    }
}
