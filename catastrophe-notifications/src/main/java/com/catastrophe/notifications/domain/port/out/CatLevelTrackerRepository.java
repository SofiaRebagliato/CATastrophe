package com.catastrophe.notifications.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Tracker del último nivel conocido por gato.
 * <p>
 * Necesario para detectar <em>level-up reales</em> al consumir {@code XpGained}.
 * El evento trae {@code newLevel} pero no el nivel previo; mantenemos aquí un
 * pequeño estado por gato (en BD, no en Redis, porque debe ser durable y
 * transaccional con la creación de la notificación).
 */
public interface CatLevelTrackerRepository {

    Optional<Integer> findLastLevel(UUID catId);

    /**
     * Guarda o actualiza el último nivel conocido del gato. La operación es
     * idempotente (upsert).
     */
    void upsert(UUID catId, int level);
}
