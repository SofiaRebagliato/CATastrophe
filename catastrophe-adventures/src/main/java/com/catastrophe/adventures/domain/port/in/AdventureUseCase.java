package com.catastrophe.adventures.domain.port.in;

import com.catastrophe.adventures.domain.model.Adventure;
import com.catastrophe.adventures.domain.model.CatAdventure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de aventuras.
 */
public interface AdventureUseCase {

    /** Listar aventuras disponibles. */
    List<Adventure> findAvailable(String type);

    /** Buscar aventura por id. */
    Optional<Adventure> findById(UUID id);

    /** Un gato inicia una aventura. */
    CatAdventure start(StartAdventureCommand command);

    /** Actualizar progreso de una aventura. */
    CatAdventure updateProgress(UUID catAdventureId, UUID catId, int progressPct);

    /** Completar una aventura manualmente. */
    CatAdventure complete(UUID catAdventureId, UUID catId);

    /** Abandonar una aventura. */
    CatAdventure abandon(UUID catAdventureId, UUID catId);

    /** Aventuras activas de un gato. */
    List<CatAdventure> findActiveByCat(UUID catId);

    /** Historial de aventuras de un gato. */
    List<CatAdventure> findHistoryByCat(UUID catId, int page, int size);

    record StartAdventureCommand(UUID catId, UUID adventureId) {}
}
