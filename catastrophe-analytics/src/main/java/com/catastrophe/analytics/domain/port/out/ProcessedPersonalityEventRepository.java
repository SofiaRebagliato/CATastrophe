package com.catastrophe.analytics.domain.port.out;

import java.util.UUID;

/**
 * Puerto de salida — Idempotencia del consumer de personalidades.
 * Mismo patrón que en {@code catastrophe-profiles} y {@code catastrophe-notifications}.
 */
public interface ProcessedPersonalityEventRepository {

    /**
     * Marca el evento como procesado. Devuelve {@code true} si era nuevo,
     * {@code false} si ya estaba registrado.
     */
    boolean markProcessed(UUID eventId, UUID catId, String eventType);
}
