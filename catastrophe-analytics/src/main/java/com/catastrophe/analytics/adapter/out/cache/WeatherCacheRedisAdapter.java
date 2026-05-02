package com.catastrophe.analytics.adapter.out.cache;

import com.catastrophe.analytics.domain.model.WeatherSnapshot;
import com.catastrophe.analytics.domain.port.out.WeatherCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Caché Redis del último weather válido por ubicación.
 * <p>
 * Clave: {@code weather:{lat}:{lon}} (redondeado a 2 decimales para
 * agrupar coordenadas vecinas). Valor: JSON del {@link WeatherSnapshot},
 * marcado con {@code source = CACHE} al recuperarlo.
 * <p>
 * TTL: 1 hora — más que eso y servir un fallback antiguo deja de ser útil.
 */
@Component
public class WeatherCacheRedisAdapter implements WeatherCachePort {

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheRedisAdapter.class);
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public WeatherCacheRedisAdapter(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WeatherSnapshot> getLast(double lat, double lon) {
        try {
            var json = redis.opsForValue().get(keyFor(lat, lon));
            if (json == null) {
                return Optional.empty();
            }
            var snapshot = objectMapper.readValue(json, WeatherSnapshot.class);
            // Marcar como CACHE: aunque internamente lo guardemos como LIVE,
            // al servirlo desde caché el origen efectivo es la caché.
            return Optional.of(new WeatherSnapshot(
                    snapshot.tempCelsius(),
                    snapshot.condition(),
                    snapshot.observedAt(),
                    WeatherSnapshot.Source.CACHE));
        } catch (Exception ex) {
            log.warn("Error leyendo weather de Redis: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(double lat, double lon, WeatherSnapshot snapshot) {
        try {
            var json = objectMapper.writeValueAsString(snapshot);
            redis.opsForValue().set(keyFor(lat, lon), json, TTL);
        } catch (Exception ex) {
            log.warn("Error escribiendo weather en Redis: {}", ex.getMessage());
            // No propagamos: el caché es best-effort, no debe romper el adapter principal.
        }
    }

    private String keyFor(double lat, double lon) {
        return "weather:%.2f:%.2f".formatted(lat, lon);
    }
}
