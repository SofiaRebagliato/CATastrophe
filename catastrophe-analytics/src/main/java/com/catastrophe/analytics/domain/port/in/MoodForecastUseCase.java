package com.catastrophe.analytics.domain.port.in;

import com.catastrophe.analytics.domain.model.MoodForecast;

import java.util.UUID;

/**
 * Puerto de entrada — Pronóstico de humor gatuno.
 */
public interface MoodForecastUseCase {

    /**
     * Genera un pronóstico de humor combinando personalidad del gato,
     * clima en (lat, lon) y una curiosidad aleatoria.
     * <p>
     * Internamente usa {@code StructuredTaskScope} para lanzar las dos
     * llamadas externas (weather y catFact) en paralelo, ya que son
     * independientes entre sí.
     */
    MoodForecast forecast(UUID catId, double lat, double lon);
}
