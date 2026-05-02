package com.catastrophe.analytics.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Pronóstico de humor gatuno — combina personalidad del gato + clima + curiosidad.
 * <p>
 * Lo construye {@code MoodForecastService} llamando en paralelo (con
 * {@code StructuredTaskScope}) a los providers de weather y cat-facts, y luego
 * combinando con la personalidad del gato.
 * <p>
 * Cualquiera de los dos componentes externos puede faltar (API caída, sin key,
 * fallback no disponible). El forecast se construye con lo que haya.
 */
public record MoodForecast(
        UUID catId,
        Optional<Trait> dominantTrait,
        Optional<WeatherSnapshot> weather,
        Optional<CatFact> fact,
        String message,
        Instant generatedAt
) {
}
