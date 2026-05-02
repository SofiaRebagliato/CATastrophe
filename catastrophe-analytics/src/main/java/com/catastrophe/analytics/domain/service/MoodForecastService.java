package com.catastrophe.analytics.domain.service;

import com.catastrophe.analytics.domain.model.CatFact;
import com.catastrophe.analytics.domain.model.MoodForecast;
import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.model.Trait;
import com.catastrophe.analytics.domain.model.WeatherSnapshot;
import com.catastrophe.analytics.domain.port.in.MoodForecastUseCase;
import com.catastrophe.analytics.domain.port.out.CatFactProvider;
import com.catastrophe.analytics.domain.port.out.PersonalityRepository;
import com.catastrophe.analytics.domain.port.out.WeatherProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Pronóstico de humor gatuno.
 * <p>
 * <strong>Aquí se luce Java 21:</strong> usa {@link StructuredTaskScope}
 * (Structured Concurrency) para lanzar las dos llamadas externas
 * (weather + cat fact) en paralelo. Es un caso de uso ideal:
 * <ul>
 *   <li>Las dos llamadas son independientes entre sí.</li>
 *   <li>Las dos van a APIs externas con latencia de red.</li>
 *   <li>Si una falla, queremos seguir con la otra (no propagar la
 *       cancelación).</li>
 * </ul>
 * <p>
 * Por eso usamos la variante <em>sin</em> {@code ShutdownOnFailure}:
 * dejamos que ambas subtareas terminen como puedan y combinamos lo que
 * tengamos. Esto es el patrón "fan-out / fan-in tolerante a fallos".
 * <p>
 * Lectura de personalidad: como es una operación local (BD), no compensa
 * lanzarla en paralelo — se hace en el hilo principal antes del scope.
 */
@Service
public class MoodForecastService implements MoodForecastUseCase {

    private static final Logger log = LoggerFactory.getLogger(MoodForecastService.class);

    private final PersonalityRepository personalityRepository;
    private final WeatherProvider weatherProvider;
    private final CatFactProvider catFactProvider;

    public MoodForecastService(PersonalityRepository personalityRepository,
                               WeatherProvider weatherProvider,
                               CatFactProvider catFactProvider) {
        this.personalityRepository = personalityRepository;
        this.weatherProvider = weatherProvider;
        this.catFactProvider = catFactProvider;
    }

    @Override
    public MoodForecast forecast(UUID catId, double lat, double lon) {
        // Personalidad: BD local, sin paralelizar
        Personality personality = personalityRepository.findByCatId(catId);

        // Weather + CatFact: APIs externas, en paralelo con structured concurrency
        Optional<WeatherSnapshot> weather;
        Optional<CatFact> fact;

        try (var scope = new StructuredTaskScope<Object>()) {
            Subtask<Optional<WeatherSnapshot>> weatherTask =
                    scope.fork(() -> weatherProvider.fetchWeather(lat, lon));
            Subtask<Optional<CatFact>> factTask =
                    scope.fork(() -> catFactProvider.fetchRandomFact());

            // Esperamos a que ambas terminen. No propagamos cancelación
            // si una falla — queremos lo que haya.
            scope.join();

            weather = resultOrEmpty(weatherTask, "weather");
            fact = resultOrEmpty(factTask, "catFact");

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Forecast interrumpido para gato {}", catId);
            weather = Optional.empty();
            fact = Optional.empty();
        }

        var dominant = personality.dominantTrait();
        var message = composeMessage(dominant, weather, fact);

        return new MoodForecast(catId, dominant, weather, fact, message, Instant.now());
    }

    /**
     * Extrae el resultado de una subtarea de forma defensiva: si terminó OK
     * devuelve su valor; si falló, lo loguea y devuelve {@link Optional#empty()}.
     */
    private <T> Optional<T> resultOrEmpty(Subtask<Optional<T>> task, String name) {
        return switch (task.state()) {
            case SUCCESS -> task.get();
            case FAILED -> {
                log.warn("Subtarea '{}' falló: {}", name, task.exception().getMessage());
                yield Optional.empty();
            }
            case UNAVAILABLE -> {
                log.warn("Subtarea '{}' no completó a tiempo", name);
                yield Optional.empty();
            }
        };
    }

    /**
     * Compone el texto del pronóstico combinando los tres ingredientes.
     * Cada uno puede faltar: el mensaje se construye con lo que haya.
     */
    private String composeMessage(Optional<Trait> dominant,
                                  Optional<WeatherSnapshot> weather,
                                  Optional<CatFact> fact) {
        var sb = new StringBuilder();

        dominant.ifPresentOrElse(
                trait -> sb.append("Hoy tu gato tiene espíritu ")
                          .append(humanize(trait))
                          .append(". "),
                () -> sb.append("Tu gato es todo un misterio aún sin descifrar. ")
        );

        weather.ifPresent(w -> sb.append("Con %.1f°C y %s, ".formatted(w.tempCelsius(), w.condition()))
                                 .append(weatherAdvice(w))
                                 .append(" "));

        fact.ifPresent(f -> sb.append("¿Sabías que ").append(f.text()).append("?"));

        return sb.toString().trim();
    }

    private String humanize(Trait trait) {
        return switch (trait) {
            case PLAYFUL    -> "juguetón";
            case LAZY       -> "perezoso";
            case HUNTER     -> "cazador";
            case SOCIAL     -> "social";
            case MYSTERIOUS -> "misterioso";
        };
    }

    private String weatherAdvice(WeatherSnapshot w) {
        if (w.tempCelsius() < 10) return "es día perfecto para arroparse en una manta.";
        if (w.tempCelsius() > 28) return "buscará el azulejo más fresco de la casa.";
        return "el día invita a la siesta cerca de la ventana.";
    }
}
