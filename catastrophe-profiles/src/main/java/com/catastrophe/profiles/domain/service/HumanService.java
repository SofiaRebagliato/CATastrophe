package com.catastrophe.profiles.domain.service;

import com.catastrophe.commons.exception.CatastropheExceptions.DuplicateResourceException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Human;
import com.catastrophe.profiles.domain.port.in.HumanUseCase;
import com.catastrophe.profiles.domain.port.out.HumanRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de dominio que implementa los casos de uso de humanos.
 * 
 * Nota: Aquí inyectamos puertos de salida (interfaces), NO implementaciones.
 * El dominio nunca sabe si la persistencia es JPA, JDBC, o un mock.
 */
@Service
@Transactional
public class HumanService implements HumanUseCase {

    private final HumanRepository humanRepository;
    private final PasswordEncoder passwordEncoder;

    public HumanService(HumanRepository humanRepository, PasswordEncoder passwordEncoder) {
        this.humanRepository = humanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Human register(RegisterHumanCommand command) {
        // Validar unicidad
        if (humanRepository.existsByUsername(command.username())) {
            throw new DuplicateResourceException("Human", "username", command.username());
        }
        if (humanRepository.existsByEmail(command.email())) {
            throw new DuplicateResourceException("Human", "email", command.email());
        }

        // Crear el modelo de dominio con la contraseña hasheada
        var human = Human.create(
                command.username(),
                command.email(),
                passwordEncoder.encode(command.password()),
                command.displayName()
        );

        return humanRepository.save(human);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Human> findById(UUID id) {
        return humanRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Human> findByUsername(String username) {
        return humanRepository.findByUsername(username);
    }

    @Override
    public Human update(UUID id, UpdateHumanCommand command) {
        var human = humanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Human", id));

        // Crear nueva instancia con los datos actualizados (inmutabilidad)
        var updated = new Human(
                human.id(),
                human.username(),
                command.email() != null ? command.email() : human.email(),
                human.passwordHash(),
                command.displayName() != null ? command.displayName() : human.displayName(),
                human.createdAt(),
                human.lastLogin(),
                human.active()
        );

        return humanRepository.save(updated);
    }

    @Override
    public Human updateLastLogin(UUID id) {
        var human = humanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Human", id));

        return humanRepository.save(human.withLastLogin(Instant.now()));
    }

    @Override
    public void deactivate(UUID id) {
        var human = humanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Human", id));

        humanRepository.save(human.deactivate());
    }
}
