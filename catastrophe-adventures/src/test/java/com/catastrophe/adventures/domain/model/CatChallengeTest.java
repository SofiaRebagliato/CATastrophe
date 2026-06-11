package com.catastrophe.adventures.domain.model;

import com.catastrophe.adventures.domain.model.CatChallenge.ChallengeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del modelo CatChallenge — transiciones de un reto PvP y mapeo
 * del enum a/desde su representaci\u00f3n en base de datos.
 */
class CatChallengeTest {

    @Test
    @DisplayName("create arranca PENDING sin oponente ni puntuaci\u00f3n")
    void createStartsPending() {
        var cc = CatChallenge.create(UUID.randomUUID(), UUID.randomUUID());

        assertThat(cc.status()).isEqualTo(ChallengeStatus.PENDING);
        assertThat(cc.opponentId()).isNull();
        assertThat(cc.score()).isZero();
    }

    @Test
    @DisplayName("accept pasa a ACTIVE y asigna oponente")
    void acceptAssignsOpponent() {
        var opponent = UUID.randomUUID();
        var cc = CatChallenge.create(UUID.randomUUID(), UUID.randomUUID()).accept(opponent);

        assertThat(cc.status()).isEqualTo(ChallengeStatus.ACTIVE);
        assertThat(cc.opponentId()).isEqualTo(opponent);
    }

    @Test
    @DisplayName("resolve fija el resultado y la puntuaci\u00f3n final")
    void resolveSetsResult() {
        var cc = CatChallenge.create(UUID.randomUUID(), UUID.randomUUID())
                .accept(UUID.randomUUID())
                .resolve(ChallengeStatus.WON, 95);

        assertThat(cc.status()).isEqualTo(ChallengeStatus.WON);
        assertThat(cc.score()).isEqualTo(95);
    }

    @Test
    @DisplayName("dbValue devuelve el nombre en min\u00fasculas")
    void dbValueIsLowercase() {
        assertThat(ChallengeStatus.WON.dbValue()).isEqualTo("won");
        assertThat(ChallengeStatus.PENDING.dbValue()).isEqualTo("pending");
    }

    @Test
    @DisplayName("fromDb reconstruye el enum desde su valor de BD")
    void fromDbParsesValue() {
        assertThat(ChallengeStatus.fromDb("draw")).isEqualTo(ChallengeStatus.DRAW);
        assertThat(ChallengeStatus.fromDb("active")).isEqualTo(ChallengeStatus.ACTIVE);
    }
}
