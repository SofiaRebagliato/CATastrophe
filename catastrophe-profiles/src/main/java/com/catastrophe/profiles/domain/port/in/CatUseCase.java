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

    /**
     * Aplica una ganancia de XP al gato. Es idempotente: si el {@code eventId}
     * ya se procesó previamente (mismo evento de gamificación replayed), no
     * vuelve a aplicar el XP.
     * <p>
     * Si la ganancia produce subida de nivel, el evento publicado lo reflejará
     * en {@code newLevel}. El consumidor de notificaciones detectará el level-up.
     *
     * @return el gato actualizado, o {@link Optional#empty()} si el evento ya
     *         estaba procesado o el gato no existe
     */
    Optional<Cat> applyXpGain(UUID eventId, UUID catId, int amount, String source);

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
