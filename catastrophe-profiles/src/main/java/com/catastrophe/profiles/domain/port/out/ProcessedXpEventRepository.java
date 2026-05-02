package com.catastrophe.profiles.domain.port.out;

import java.util.UUID;

/**
 * Puerto de salida — Tracker de eventos XP ya procesados.
 * <p>
 * Garantiza idempotencia del consumer Kafka que aplica XP a los gatos:
 * si un evento de gamificación llega dos veces (reintento, rebalanceo del
 * consumer group), la segunda aplicación no duplica el XP.
 * <p>
 * El adaptador concreto se apoya en una tabla con {@code event_id} como PK
 * y aprovecha la atomicidad del INSERT para detectar el conflicto.
 */
public interface ProcessedXpEventRepository {

    /**
     * Marca el evento como procesado. Devuelve {@code true} si era nuevo,
     * {@code false} si ya estaba registrado (procesado anteriormente).
     */
    boolean markProcessed(UUID eventId, UUID catId, int amount, String source);
}
