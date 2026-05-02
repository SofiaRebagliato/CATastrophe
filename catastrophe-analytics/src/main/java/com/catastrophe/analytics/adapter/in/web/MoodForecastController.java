package com.catastrophe.analytics.adapter.in.web;

import com.catastrophe.analytics.domain.model.CatFact;
import com.catastrophe.analytics.domain.model.MoodForecast;
import com.catastrophe.analytics.domain.model.Trait;
import com.catastrophe.analytics.domain.model.WeatherSnapshot;
import com.catastrophe.analytics.domain.port.in.MoodForecastUseCase;
import com.catastrophe.analytics.domain.port.out.CatFactProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Endpoints de "humor del día" — pronóstico combinado y curiosidades sueltas.
 */
@RestController
public class MoodForecastController {

    private final MoodForecastUseCase forecastUseCase;
    private final CatFactProvider catFactProvider;

    public MoodForecastController(MoodForecastUseCase forecastUseCase,
                                  CatFactProvider catFactProvider) {
        this.forecastUseCase = forecastUseCase;
        this.catFactProvider = catFactProvider;
    }

    @GetMapping("/api/v1/mood-forecast")
    public ResponseEntity<MoodForecastResponse> forecast(
            @RequestParam UUID catId,
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(MoodForecastResponse.from(
                forecastUseCase.forecast(catId, lat, lon)));
    }

    @GetMapping("/api/v1/cat-facts/random")
    public ResponseEntity<CatFactResponse> randomFact() {
        var fact = catFactProvider.fetchRandomFact();
        return fact.map(f -> ResponseEntity.ok(new CatFactResponse(f.text())))
                   .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record MoodForecastResponse(
            UUID catId,
            Trait dominantTrait,
            WeatherSnapshot weather,
            CatFact fact,
            String message,
            Instant generatedAt
    ) {
        static MoodForecastResponse from(MoodForecast f) {
            return new MoodForecastResponse(
                    f.catId(),
                    f.dominantTrait().orElse(null),
                    f.weather().orElse(null),
                    f.fact().orElse(null),
                    f.message(),
                    f.generatedAt()
            );
        }
    }

    public record CatFactResponse(String text) {}
}
