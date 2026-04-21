package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.CatAdventure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatAdventureRepository {
    CatAdventure save(CatAdventure catAdventure);
    Optional<CatAdventure> findById(UUID id);
    List<CatAdventure> findActiveByCatId(UUID catId);
    List<CatAdventure> findByCatId(UUID catId, int page, int size);
    boolean existsActive(UUID catId, UUID adventureId);
}
