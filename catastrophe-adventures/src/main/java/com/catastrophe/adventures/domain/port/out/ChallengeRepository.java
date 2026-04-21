package com.catastrophe.adventures.domain.port.out;

import com.catastrophe.adventures.domain.model.Challenge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeRepository {
    List<Challenge> findAll();
    Optional<Challenge> findById(UUID id);
}
