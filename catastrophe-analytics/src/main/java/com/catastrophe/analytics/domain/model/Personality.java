package com.catastrophe.analytics.domain.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Personalidad completa de un gato — los 5 traits con su score actual.
 * <p>
 * Modelo agregado: en BD vive una fila por trait, pero el dominio
 * trabaja con el conjunto. El service ensambla un {@code Personality}
 * leyendo todas las filas del gato y reconstruyendo este record.
 */
public record Personality(
        UUID catId,
        Map<Trait, Double> scores,
        Instant updatedAt
) {

    public Personality {
        Objects.requireNonNull(catId, "catId");
        Objects.requireNonNull(scores, "scores");
        // Garantizamos un EnumMap inmutable para que el record sea seguro.
        scores = Map.copyOf(scores);
    }

    /**
     * Crea una personalidad inicial con todos los traits a 0.
     */
    public static Personality empty(UUID catId) {
        var map = new EnumMap<Trait, Double>(Trait.class);
        for (var t : Trait.values()) {
            map.put(t, 0.0);
        }
        return new Personality(catId, map, Instant.now());
    }

    /**
     * Score de un trait concreto. Si no existe, devuelve 0.
     */
    public double scoreOf(Trait trait) {
        return scores.getOrDefault(trait, 0.0);
    }

    /**
     * Trait dominante: el de mayor score. Si todos están a 0 (gato sin actividad),
     * devuelve {@link Optional#empty()}.
     */
    public Optional<Trait> dominantTrait() {
        return scores.entrySet().stream()
                .filter(e -> e.getValue() > 0.0)
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }
}
