package com.catastrophe.adventures.adapter.out.cache;

import com.catastrophe.adventures.domain.model.RankingEntry;
import com.catastrophe.adventures.domain.port.out.RankingCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador de salida — Rankings con Redis Sorted Sets.
 *
 * Cada ranking es un Sorted Set donde:
 *  - member = catId (UUID como string)
 *  - score  = valor numérico (XP total, nº de badges, etc.)
 *
 * Redis ZREVRANGE devuelve los miembros de mayor a menor score,
 * ideal para rankings "top N".
 */
@Component
public class RedisRankingAdapter implements RankingCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisRankingAdapter.class);

    private final ZSetOperations<String, String> zSetOps;

    public RedisRankingAdapter(StringRedisTemplate redisTemplate) {
        this.zSetOps = redisTemplate.opsForZSet();
    }

    @Override
    public void updateScore(String rankingKey, UUID catId, double score) {
        log.debug("Actualizando ranking '{}': cat={}, score={}", rankingKey, catId, score);
        zSetOps.add(rankingKey, catId.toString(), score);
    }

    @Override
    public List<RankingEntry> getTopN(String rankingKey, int n) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                zSetOps.reverseRangeWithScores(rankingKey, 0, n - 1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        var entries = new ArrayList<RankingEntry>();
        int rank = 1;
        for (var tuple : tuples) {
            entries.add(new RankingEntry(
                    rank++,
                    UUID.fromString(tuple.getValue()),
                    tuple.getScore() != null ? tuple.getScore() : 0
            ));
        }
        return entries;
    }

    @Override
    public RankingEntry getRank(String rankingKey, UUID catId) {
        Long rank = zSetOps.reverseRank(rankingKey, catId.toString());
        Double score = zSetOps.score(rankingKey, catId.toString());

        if (rank == null) {
            // El gato no está en el ranking aún
            return new RankingEntry(-1, catId, 0);
        }

        return new RankingEntry(
                rank.intValue() + 1, // ZREVRANK es 0-based
                catId,
                score != null ? score : 0
        );
    }
}
