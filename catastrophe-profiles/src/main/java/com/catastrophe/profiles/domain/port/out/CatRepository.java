package com.catastrophe.profiles.domain.port.out;

import com.catastrophe.profiles.domain.model.Cat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de gatos.
 */
public interface CatRepository {

    Cat save(Cat cat);

    Optional<Cat> findById(UUID id);

    List<Cat> findByHumanId(UUID humanId);

    boolean existsByHumanIdAndName(UUID humanId, String name);

    void deleteById(UUID id);
}
