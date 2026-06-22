package com.catastrophe.analytics.adapter.out.external;

import com.catastrophe.analytics.domain.model.WeatherSnapshot;
import com.catastrophe.analytics.domain.port.out.WeatherCachePort;
import com.catastrophe.analytics.domain.port.out.WeatherProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador OpenWeatherMap con tolerancia a fallos.
 * <p>
 * <strong>Capas de defensa</strong> (en orden):
 * <ol>
 *   <li>Si no hay API key configurada → devolvemos el último cacheado en Redis.
 *       En dev no necesitas key para que el servicio funcione.</li>
 *   <li>Llamada a la API real con {@code @CircuitBreaker}: tras N fallos consecutivos
 *       el circuito se abre y todas las llamadas siguientes van directas al fallback
 *       hasta que el circuito se cierre de nuevo.</li>
 *   <li>Si la llamada falla (excepción o timeout), el {@code fallbackMethod} sirve
 *       el último weather válido cacheado en Redis.</li>
 *   <li>Si no hay nada en Redis tampoco, devolvemos {@link Optional#empty()} y
 *       {@code MoodForecastService} compone el mensaje sin clima.</li>
 * </ol>
 * <p>
 * Cuando la llamada tiene éxito, escribimos el resultado en la caché para que
 * próximos fallos puedan usarlo como fallback.
 */
@Component
public class OpenWeatherMapAdapter implements WeatherProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherMapAdapter.class);
    private static final String CB_NAME = "openweathermap";

    private final RestClient restClient;
    private final WeatherCachePort cache;
    private final String apiKey;

    public OpenWeatherMapAdapter(
            @Value("${catastrophe.openweather.base-url:https://api.openweathermap.org/data/2.5}") String baseUrl,
            @Value("${catastrophe.openweather.api-key:}") String apiKey,
            WeatherCachePort cache) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.cache = cache;
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "fetchFromCache")
    @SuppressWarnings("unchecked")
    public Optional<WeatherSnapshot> fetchWeather(double lat, double lon) {
        // Sin API key → no llamamos. En dev se acepta y servimos del fallback.
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenWeatherMap sin API key, sirviendo de caché si existe");
            return cache.getLast(lat, lon);
        }

        // OJO: construimos la query con el uriBuilder en lugar de String.formatted("%f").
        // Spring convierte los double con Double.toString() (independiente del locale),
        // así evitamos que en una máquina con locale es-ES las coordenadas salgan con
        // coma decimal (lat=39,47 en vez de 39.47) y OpenWeatherMap responda 400.
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("units", "metric")
                        .queryParam("appid", apiKey)
                        .build())
                .retrieve()
                .body(Map.class);

        if (response == null) {
            return Optional.empty();
        }

        var snapshot = parse(response);
        // Refrescamos la caché para próximos fallbacks
        cache.put(lat, lon, snapshot);
        return Optional.of(snapshot);
    }

    /**
     * Fallback invocado por Resilience4j cuando la llamada principal falla
     * o el circuito está abierto. La firma debe coincidir con el método
     * principal más un parámetro {@code Throwable} al final.
     */
    @SuppressWarnings("unused")
    private Optional<WeatherSnapshot> fetchFromCache(double lat, double lon, Throwable ex) {
        log.warn("Fallback OpenWeatherMap activado: {}", ex.getMessage());
        return cache.getLast(lat, lon);
    }

    /**
     * Parser tolerante: si falta algún campo opcional usamos valores neutros.
     */
    @SuppressWarnings("unchecked")
    private WeatherSnapshot parse(Map<String, Object> response) {
        var main = (Map<String, Object>) response.getOrDefault("main", Map.of());
        var weatherList = (java.util.List<Map<String, Object>>) response.getOrDefault("weather", java.util.List.of());

        double temp = ((Number) main.getOrDefault("temp", 20.0)).doubleValue();
        String condition = weatherList.isEmpty()
                ? "Desconocido"
                : (String) weatherList.get(0).getOrDefault("description", "Desconocido");

        return new WeatherSnapshot(temp, condition, Instant.now(), WeatherSnapshot.Source.LIVE);
    }
}
