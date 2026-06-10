package com.catastrophe.adventures.domain.port.in;

import com.catastrophe.adventures.domain.model.Badge;
import com.catastrophe.adventures.domain.model.CatBadge;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de badges/logros.
 */
public interface BadgeUseCase {

    /** Listar todos los badges disponibles. */
    List<Badge> findAll();

    /** Buscar badge por ID. */
    java.util.Optional<Badge> findById(UUID badgeId);

    /** Badges ganados por un gato. */
    List<CatBadge> findByCat(UUID catId);

    /** Otorgar un badge a un gato (si no lo tiene ya). */
    CatBadge award(UUID catId, UUID badgeId);
}
