package com.catastrophe.analytics.domain.port.out;

import com.catastrophe.analytics.domain.model.WeatherSnapshot;

import java.util.Optional;

/**
 * Puerto de salida — Proveedor de información meteorológica.
 * <p>
 * Implementaciones esperadas:
 * <ul>
 *   <li>{@code OpenWeatherMapAdapter} — llamada real con Resilience4j Circuit Breaker.</li>
 *   <li>Fallback automático a la caché Redis cuando el adapter falla o no hay API key.</li>
 * </ul>
 * Devuelve {@link Optional#empty()} si no hay dato disponible (ni live ni cacheado).
 */
public interface WeatherProvider {

    Optional<WeatherSnapshot> fetchWeather(double lat, double lon);
}
