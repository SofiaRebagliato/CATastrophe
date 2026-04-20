package com.catastrophe.profiles.domain.port.out;

import com.catastrophe.profiles.domain.model.Human;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de humanos.
 */
public interface HumanRepository {

    Human save(Human human);

    Optional<Human> findById(UUID id);

    Optional<Human> findByUsername(String username);

    Optional<Human> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}
