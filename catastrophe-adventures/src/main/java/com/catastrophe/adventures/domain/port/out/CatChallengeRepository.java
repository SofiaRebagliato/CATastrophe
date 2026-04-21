package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.CatChallenge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatChallengeRepository {
    CatChallenge save(CatChallenge catChallenge);
    Optional<CatChallenge> findById(UUID id);
    List<CatChallenge> findPendingByChallenge(UUID challengeId);
    List<CatChallenge> findByCatId(UUID catId);
}
