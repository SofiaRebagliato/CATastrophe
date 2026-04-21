package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.RankingEntry;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida — Caché de rankings (Redis Sorted Sets).
 */
public interface RankingCachePort {
    void updateScore(String rankingKey, UUID catId, double score);
    List<RankingEntry> getTopN(String rankingKey, int n);
    RankingEntry getRank(String rankingKey, UUID catId);
}
