package com.catastrophe.adventures.domain.port.in;

import com.catastrophe.adventures.domain.model.CatChallenge;
import com.catastrophe.adventures.domain.model.Challenge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de retos PvP.
 */
public interface ChallengeUseCase {

    /** Listar retos disponibles. */
    List<Challenge> findAll();

    /** Buscar reto por id. */
    Optional<Challenge> findById(UUID id);

    /** Un gato crea un reto abierto (esperando rival). */
    CatChallenge create(UUID catId, UUID challengeId);

    /** Un gato acepta un reto abierto como oponente. */
    CatChallenge accept(UUID catChallengeId, UUID opponentId);

    /** Resolver un reto con puntuaciones. */
    ResolveResult resolve(UUID catChallengeId, int challengerScore, int opponentScore);

    /** Retos pendientes que esperan oponente. */
    List<CatChallenge> findPending(UUID challengeId);

    /** Historial de retos de un gato. */
    List<CatChallenge> findByCat(UUID catId);

    record ResolveResult(CatChallenge challenger, CatChallenge opponent) {}
}
