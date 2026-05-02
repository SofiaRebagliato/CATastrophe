package com.catastrophe.analytics.domain.port.out;

import com.catastrophe.analytics.domain.model.Personality;
import com.catastrophe.analytics.domain.model.Trait;

import java.util.UUID;

/**
 * Puerto de salida — Persistencia de personalidades.
 * <p>
 * El adaptador concreto se apoya en una tabla {@code cat_personalities}
 * con índice único en {@code (cat_id, trait)} para que cada trait se
 * actualice in-place.
 */
public interface PersonalityRepository {

    Personality findByCatId(UUID catId);

    /**
     * Upsert atómico de un score: si la fila {@code (cat_id, trait)} no existe
     * la crea, si existe actualiza el {@code score} y {@code updated_at}.
     */
    void upsertScore(UUID catId, Trait trait, double score);
}
