package com.catastrophe.adventures.domain.port.in;

import com.catastrophe.adventures.domain.model.RankingEntry;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de rankings.
 *
 * Los rankings se almacenan en Redis Sorted Sets para
 * lectura ultrarrápida y actualización en tiempo real.
 */
public interface RankingUseCase {

    /** Top N gatos por XP global. */
    List<RankingEntry> getGlobalRanking(int top);

    /** Top N gatos por número de badges. */
    List<RankingEntry> getBadgeRanking(int top);

    /** Posición de un gato en el ranking global. */
    RankingEntry getCatRank(UUID catId);

    /** Actualizar puntuación de un gato en el ranking. */
    void updateScore(UUID catId, double score);

    /** Actualizar puntuación de badges de un gato. */
    void updateBadgeScore(UUID catId, double score);
}
