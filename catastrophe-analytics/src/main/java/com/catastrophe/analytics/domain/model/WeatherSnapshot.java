package com.catastrophe.analytics.domain.model;

import java.time.Instant;

/**
 * Lectura de clima en un momento dado, devuelta por {@code WeatherProvider}.
 * <p>
 * Modelo simplificado — solo guardamos lo que la spec usa para el
 * "pronóstico de humor gatuno": temperatura, condición meteorológica,
 * y de qué fuente proviene (real o caché de fallback).
 */
public record WeatherSnapshot(
        double tempCelsius,
        String condition,
        Instant observedAt,
        Source source
) {

    public enum Source {
        /** Datos frescos de la API de OpenWeatherMap. */
        LIVE,
        /** Datos servidos desde la caché Redis (último válido). */
        CACHE
    }
}
