package com.catastrophe.profiles.domain.port.in;

import com.catastrophe.profiles.domain.model.Human;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada — Define los casos de uso de gestión de humanos.
 * 
 * En arquitectura hexagonal, los puertos de entrada son interfaces
 * que la capa de dominio expone y que los adaptadores de entrada
 * (controllers, CLI, etc.) invocan.
 */
public interface HumanUseCase {

    /** Registrar un nuevo humano en la plataforma. */
    Human register(RegisterHumanCommand command);

    /** Buscar humano por id. */
    Optional<Human> findById(UUID id);

    /** Buscar humano por username. */
    Optional<Human> findByUsername(String username);

    /** Buscar humanos activos por username o nombre visible (case-insensitive). */
    java.util.List<Human> search(String query, int limit);

    /** Actualizar datos del humano. */
    Human update(UUID id, UpdateHumanCommand command);

    /** Actualizar el timestamp de último login. */
    Human updateLastLogin(UUID id);

    /** Desactivar cuenta. */
    void deactivate(UUID id);

    // ── Commands (records inmutables) ──

    record RegisterHumanCommand(
            String username,
            String email,
            String password,
            String displayName
    ) {}

    record UpdateHumanCommand(
            String displayName,
            String email
    ) {}
}
