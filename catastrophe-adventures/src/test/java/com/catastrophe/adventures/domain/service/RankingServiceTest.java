package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.RankingEntry;
import com.catastrophe.adventures.domain.port.out.RankingCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de RankingService — comprueba que cada operaci\u00f3n delega
 * en la clave de Redis correcta (ranking:global vs. ranking:badges).
 */
class RankingServiceTest {

    private RankingCachePort rankingCache;
    private RankingService service;

    private final UUID catId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        rankingCache = mock(RankingCachePort.class);
        service = new RankingService(rankingCache);
    }

    @Test
    @DisplayName("getGlobalRanking consulta la clave ranking:global")
    void globalRankingUsesGlobalKey() {
        when(rankingCache.getTopN("ranking:global", 10)).thenReturn(List.of());
        service.getGlobalRanking(10);
        verify(rankingCache).getTopN("ranking:global", 10);
    }

    @Test
    @DisplayName("getBadgeRanking consulta la clave ranking:badges")
    void badgeRankingUsesBadgeKey() {
        when(rankingCache.getTopN("ranking:badges", 5)).thenReturn(List.of());
        service.getBadgeRanking(5);
        verify(rankingCache).getTopN("ranking:badges", 5);
    }

    @Test
    @DisplayName("getCatRank consulta el rango en ranking:global")
    void catRankUsesGlobalKey() {
        when(rankingCache.getRank("ranking:global", catId)).thenReturn(new RankingEntry(1, catId, 100));
        service.getCatRank(catId);
        verify(rankingCache).getRank("ranking:global", catId);
    }

    @Test
    @DisplayName("updateScore escribe en ranking:global")
    void updateScoreUsesGlobalKey() {
        service.updateScore(catId, 123.0);
        verify(rankingCache).updateScore("ranking:global", catId, 123.0);
    }

    @Test
    @DisplayName("updateBadgeScore escribe en ranking:badges")
    void updateBadgeScoreUsesBadgeKey() {
        service.updateBadgeScore(catId, 7.0);
        verify(rankingCache).updateScore("ranking:badges", catId, 7.0);
    }
}
