package com.catastrophe.adventures.domain.model;

import java.util.UUID;

/**
 * Entrada de ranking — posición de un gato en una clasificación.
 */
public record RankingEntry(
        int rank,
        UUID catId,
        double score
) {}
