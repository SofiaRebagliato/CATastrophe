package com.catastrophe.analytics.domain.port.out;

import com.catastrophe.analytics.domain.model.CatFact;

import java.util.Optional;

/**
 * Puerto de salida — Proveedor de curiosidades felinas.
 * <p>
 * Implementación: {@code CatFactAdapter} con un pool fijo hardcoded
 * (20 curiosidades) que sirve siempre, complementado por una caché Redis
 * que puede llenarse desde la API real (catfact.ninja) cuando esté disponible.
 */
public interface CatFactProvider {

    /**
     * Devuelve una curiosidad aleatoria. Idealmente nunca falla (el pool
     * fijo siempre está disponible), pero la firma es {@code Optional} por
     * consistencia con el resto de providers externos.
     */
    Optional<CatFact> fetchRandomFact();
}
