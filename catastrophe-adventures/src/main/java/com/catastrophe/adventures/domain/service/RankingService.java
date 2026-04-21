package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.RankingEntry;
import com.catastrophe.adventures.domain.port.in.RankingUseCase;
import com.catastrophe.adventures.domain.port.out.RankingCachePort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de dominio — Rankings en tiempo real.
 *
 * Utiliza Redis Sorted Sets para mantener clasificaciones
 * actualizadas de forma eficiente. Cada actualización de XP
 * o badge se refleja instantáneamente en el ranking.
 */
@Service
public class RankingService implements RankingUseCase {

    private static final String RANKING_GLOBAL = "ranking:global";
    private static final String RANKING_BADGES = "ranking:badges";

    private final RankingCachePort rankingCache;

    public RankingService(RankingCachePort rankingCache) {
        this.rankingCache = rankingCache;
    }

    @Override
    public List<RankingEntry> getGlobalRanking(int top) {
        return rankingCache.getTopN(RANKING_GLOBAL, top);
    }

    @Override
    public List<RankingEntry> getBadgeRanking(int top) {
        return rankingCache.getTopN(RANKING_BADGES, top);
    }

    @Override
    public RankingEntry getCatRank(UUID catId) {
        return rankingCache.getRank(RANKING_GLOBAL, catId);
    }

    @Override
    public void updateScore(UUID catId, double score) {
        rankingCache.updateScore(RANKING_GLOBAL, catId, score);
    }

    @Override
    public void updateBadgeScore(UUID catId, double score) {
        rankingCache.updateScore(RANKING_BADGES, catId, score);
    }
}
