package com.catastrophe.profiles.domain.port.in;

import com.catastrophe.profiles.domain.model.Cat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada — Define los casos de uso de gestión de gatos.
 */
public interface CatUseCase {

    /** Crear un nuevo gato asociado a un humano. */
    Cat create(CreateCatCommand command);

    /** Buscar gato por id. */
    Optional<Cat> findById(UUID id);

    /** Buscar gatos de un humano. */
    List<Cat> findByHumanId(UUID humanId);

    /** Actualizar datos de un gato. */
    Cat update(UUID catId, UpdateCatCommand command);

    /** Obtener un nuevo avatar aleatorio de TheCatAPI. */
    Cat refreshAvatar(UUID catId);

    /** Eliminar un gato. */
    void delete(UUID catId);

    // ── Commands (records inmutables) ──

    record CreateCatCommand(
            UUID humanId,
            String name,
            String breed,
            Integer ageMonths,
            String bio
    ) {}

    record UpdateCatCommand(
            String name,
            String breed,
            Integer ageMonths,
            String bio
    ) {}
}
