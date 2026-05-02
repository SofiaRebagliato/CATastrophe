package com.catastrophe.analytics.domain.port.out;

import com.catastrophe.analytics.domain.model.WeatherSnapshot;

import java.util.Optional;

/**
 * Puerto de salida — Caché del último weather válido por ubicación.
 * <p>
 * Implementación: Redis con TTL. Lo escribe el adapter de OpenWeatherMap cada
 * vez que tiene una respuesta válida; lo lee como fallback cuando la API falla
 * o cuando no hay API key configurada.
 */
public interface WeatherCachePort {

    Optional<WeatherSnapshot> getLast(double lat, double lon);

    void put(double lat, double lon, WeatherSnapshot snapshot);
}
