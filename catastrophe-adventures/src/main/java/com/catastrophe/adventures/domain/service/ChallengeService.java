package com.catastrophe.adventures.domain.service;

import com.catastrophe.adventures.domain.model.CatChallenge;
import com.catastrophe.adventures.domain.model.CatChallenge.ChallengeStatus;
import com.catastrophe.adventures.domain.model.Challenge;
import com.catastrophe.commons.event.CatastropheEvent.ChallengeCompleted;
import com.catastrophe.commons.event.ChallengeResult;
import com.catastrophe.commons.exception.CatastropheExceptions.*;
import com.catastrophe.adventures.domain.port.in.ChallengeUseCase;
import com.catastrophe.adventures.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de retos PvP.
 *
 * Flujo de un reto:
 * 1. Un gato crea un reto (estado PENDING, sin oponente)
 * 2. Otro gato lo acepta (estado ACTIVE, se asigna oponente)
 * 3. Se resuelve con puntuaciones → el ganador recibe más XP
 */
@Service
@Transactional
public class ChallengeService implements ChallengeUseCase {

    private final ChallengeRepository challengeRepository;
    private final CatChallengeRepository catChallengeRepository;
    private final EventPublisher eventPublisher;

    public ChallengeService(ChallengeRepository challengeRepository,
                            CatChallengeRepository catChallengeRepository,
                            EventPublisher eventPublisher) {
        this.challengeRepository = challengeRepository;
        this.catChallengeRepository = catChallengeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Challenge> findAll() {
        return challengeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Challenge> findById(UUID id) {
        return challengeRepository.findById(id);
    }

    @Override
    public CatChallenge create(UUID catId, UUID challengeId) {
        challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge", challengeId));

        var catChallenge = CatChallenge.create(catId, challengeId);
        return catChallengeRepository.save(catChallenge);
    }

    @Override
    public CatChallenge accept(UUID catChallengeId, UUID opponentId) {
        var catChallenge = catChallengeRepository.findById(catChallengeId)
                .orElseThrow(() -> new ResourceNotFoundException("CatChallenge", catChallengeId));

        if (catChallenge.status() != ChallengeStatus.PENDING) {
            throw new BusinessRuleViolationException(
                    "CHALLENGE_NOT_PENDING",
                    "Este reto ya no está disponible. ¡Busca otro rival!"
            );
        }

        if (catChallenge.catId().equals(opponentId)) {
            throw new BusinessRuleViolationException(
                    "CHALLENGE_SELF",
                    "¡Un gato no puede retarse a sí mismo! Bueno, quizás sí, pero aquí no."
            );
        }

        var accepted = catChallenge.accept(opponentId);
        return catChallengeRepository.save(accepted);
    }

    @Override
    public ResolveResult resolve(UUID catChallengeId, int challengerScore, int opponentScore) {
        var catChallenge = catChallengeRepository.findById(catChallengeId)
                .orElseThrow(() -> new ResourceNotFoundException("CatChallenge", catChallengeId));

        if (catChallenge.status() != ChallengeStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "CHALLENGE_NOT_ACTIVE",
                    "Este reto no está activo. No se puede resolver."
            );
        }

        var challenge = challengeRepository.findById(catChallenge.challengeId()).orElseThrow();

        // Determinar resultado
        ChallengeStatus challengerStatus;
        ChallengeStatus opponentStatus;
        ChallengeResult challengerResult;
        ChallengeResult opponentResult;

        if (challengerScore > opponentScore) {
            challengerStatus = ChallengeStatus.WON;
            opponentStatus = ChallengeStatus.LOST;
            challengerResult = ChallengeResult.WON;
            opponentResult = ChallengeResult.LOST;
        } else if (challengerScore < opponentScore) {
            challengerStatus = ChallengeStatus.LOST;
            opponentStatus = ChallengeStatus.WON;
            challengerResult = ChallengeResult.LOST;
            opponentResult = ChallengeResult.WON;
        } else {
            challengerStatus = ChallengeStatus.DRAW;
            opponentStatus = ChallengeStatus.DRAW;
            challengerResult = ChallengeResult.DRAW;
            opponentResult = ChallengeResult.DRAW;
        }

        var resolvedChallenger = catChallenge.resolve(challengerStatus, challengerScore);
        var savedChallenger = catChallengeRepository.save(resolvedChallenger);

        // Crear y guardar registro del oponente
        var opponentEntry = new CatChallenge(
                UUID.randomUUID(), catChallenge.opponentId(), catChallenge.challengeId(),
                catChallenge.catId(), opponentStatus, opponentScore, catChallenge.createdAt()
        );
        var savedOpponent = catChallengeRepository.save(opponentEntry);

        // XP: ganador recibe full, perdedor recibe la mitad, empate 75%
        int winnerXp = challenge.xpReward();
        int loserXp = challenge.xpReward() / 2;
        int drawXp = (int) (challenge.xpReward() * 0.75);

        int challengerXp = switch (challengerResult) {
            case WON -> winnerXp;
            case LOST -> loserXp;
            case DRAW -> drawXp;
        };
        int opponentXp = switch (opponentResult) {
            case WON -> winnerXp;
            case LOST -> loserXp;
            case DRAW -> drawXp;
        };

        // Publicar eventos para ambos participantes
        eventPublisher.publish(new ChallengeCompleted(
                UUID.randomUUID(), Instant.now(),
                catChallenge.catId(), catChallenge.challengeId(),
                catChallenge.opponentId(), challengerResult,
                challengerScore, challengerXp
        ));
        eventPublisher.publish(new ChallengeCompleted(
                UUID.randomUUID(), Instant.now(),
                catChallenge.opponentId(), catChallenge.challengeId(),
                catChallenge.catId(), opponentResult,
                opponentScore, opponentXp
        ));

        return new ResolveResult(savedChallenger, savedOpponent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatChallenge> findPending(UUID challengeId) {
        return catChallengeRepository.findPendingByChallenge(challengeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatChallenge> findByCat(UUID catId) {
        return catChallengeRepository.findByCatId(catId);
    }
}
