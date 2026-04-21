package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.Adventure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdventureRepository {
    List<Adventure> findByType(String type);
    List<Adventure> findAll();
    Optional<Adventure> findById(UUID id);
}
