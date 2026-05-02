package com.catastrophe.analytics.domain.service;

import com.catastrophe.analytics.domain.model.CatFact;
import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.model.Trait;
import com.catastrophe.analytics.domain.model.WeatherSnapshot;
import com.catastrophe.analytics.domain.port.out.CatFactProvider;
import com.catastrophe.analytics.domain.port.out.PersonalityRepository;
import com.catastrophe.analytics.domain.port.out.WeatherProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del MoodForecastService — verifica el fan-out paralelo con
 * StructuredTaskScope y la tolerancia a fallos parciales.
 */
class MoodForecastServiceTest {

    private PersonalityRepository personalityRepository;
    private WeatherProvider weatherProvider;
    private CatFactProvider catFactProvider;
    private MoodForecastService service;

    private final UUID catId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        personalityRepository = mock(PersonalityRepository.class);
        weatherProvider = mock(WeatherProvider.class);
        catFactProvider = mock(CatFactProvider.class);
        service = new MoodForecastService(personalityRepository, weatherProvider, catFactProvider);
    }

    @Test
    void forecast_combines_all_three_when_everything_works() {
        var scores = new EnumMap<Trait, Double>(Trait.class);
        scores.put(Trait.PLAYFUL, 0.8);
        scores.put(Trait.LAZY, 0.1);
        scores.put(Trait.HUNTER, 0.4);
        scores.put(Trait.SOCIAL, 0.3);
        scores.put(Trait.MYSTERIOUS, 0.2);
        var personality = new Personality(catId, scores, Instant.now());

        var weather = new WeatherSnapshot(15.0, "soleado",
                Instant.now(), WeatherSnapshot.Source.LIVE);
        var fact = new CatFact("los gatos ronronean a 25 Hz");

        when(personalityRepository.findByCatId(catId)).thenReturn(personality);
        when(weatherProvider.fetchWeather(anyDouble(), anyDouble())).thenReturn(Optional.of(weather));
        when(catFactProvider.fetchRandomFact()).thenReturn(Optional.of(fact));

        var result = service.forecast(catId, 40.4, -3.7);

        assertThat(result.catId()).isEqualTo(catId);
        assertThat(result.dominantTrait()).contains(Trait.PLAYFUL);
        assertThat(result.weather()).contains(weather);
        assertThat(result.fact()).contains(fact);
        assertThat(result.message()).contains("juguetón");
        assertThat(result.message()).contains("15");
        assertThat(result.message()).contains("ronronean");
    }

    @Test
    void forecast_works_when_weather_is_unavailable() {
        when(personalityRepository.findByCatId(catId)).thenReturn(Personality.empty(catId));
        when(weatherProvider.fetchWeather(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(catFactProvider.fetchRandomFact()).thenReturn(Optional.of(new CatFact("dato curioso")));

        var result = service.forecast(catId, 0.0, 0.0);

        assertThat(result.weather()).isEmpty();
        assertThat(result.fact()).isPresent();
        assertThat(result.message()).contains("dato curioso");
    }

    @Test
    void forecast_works_when_fact_is_unavailable() {
        var personality = personalityWith(Trait.HUNTER, 0.9);
        when(personalityRepository.findByCatId(catId)).thenReturn(personality);
        when(weatherProvider.fetchWeather(anyDouble(), anyDouble())).thenReturn(
                Optional.of(new WeatherSnapshot(30.0, "calor", Instant.now(), WeatherSnapshot.Source.LIVE)));
        when(catFactProvider.fetchRandomFact()).thenReturn(Optional.empty());

        var result = service.forecast(catId, 1.0, 1.0);

        assertThat(result.fact()).isEmpty();
        assertThat(result.weather()).isPresent();
        assertThat(result.message()).contains("cazador");
    }

    @Test
    void forecast_survives_when_provider_throws() {
        when(personalityRepository.findByCatId(catId)).thenReturn(Personality.empty(catId));
        when(weatherProvider.fetchWeather(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("API down"));
        when(catFactProvider.fetchRandomFact()).thenReturn(Optional.of(new CatFact("ok")));

        // No debe propagar la excepción
        var result = service.forecast(catId, 0.0, 0.0);

        assertThat(result.weather()).isEmpty();
        assertThat(result.fact()).isPresent();
    }

    @Test
    void forecast_returns_empty_dominant_when_personality_is_blank() {
        when(personalityRepository.findByCatId(catId)).thenReturn(Personality.empty(catId));
        when(weatherProvider.fetchWeather(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(catFactProvider.fetchRandomFact()).thenReturn(Optional.empty());

        var result = service.forecast(catId, 0.0, 0.0);

        assertThat(result.dominantTrait()).isEmpty();
        assertThat(result.message()).contains("misterio");
    }

    private Personality personalityWith(Trait trait, double score) {
        var scores = new EnumMap<Trait, Double>(Trait.class);
        for (var t : Trait.values()) {
            scores.put(t, 0.0);
        }
        scores.put(trait, score);
        return new Personality(catId, scores, Instant.now());
    }
}
