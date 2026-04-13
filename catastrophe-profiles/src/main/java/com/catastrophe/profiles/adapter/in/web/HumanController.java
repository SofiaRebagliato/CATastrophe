package com.catastrophe.profiles.adapter.in.web;

import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Human;
import com.catastrophe.profiles.domain.port.in.HumanUseCase;
import com.catastrophe.profiles.domain.port.in.HumanUseCase.UpdateHumanCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Gestión de perfiles de humanos.
 *
 * Los humanos son "asistentes" de sus gatos. Este controller
 * permite ver y editar el perfil propio.
 */
@RestController
@RequestMapping("/api/v1/humans")
public class HumanController {

    private final HumanUseCase humanUseCase;

    public HumanController(HumanUseCase humanUseCase) {
        this.humanUseCase = humanUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<HumanResponse> findById(@PathVariable UUID id) {
        return humanUseCase.findById(id)
                .map(h -> ResponseEntity.ok(HumanResponse.from(h)))
                .orElseThrow(() -> new ResourceNotFoundException("Human", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HumanResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateHumanRequest request,
                                                 Authentication authentication) {
        // Verificar que el usuario autenticado es el dueño del perfil
        var human = humanUseCase.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Human", id));

        if (!human.username().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        var command = new UpdateHumanCommand(
                request.displayName(),
                request.email()
        );
        var updated = humanUseCase.update(id, command);
        return ResponseEntity.ok(HumanResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id,
                                            Authentication authentication) {
        var human = humanUseCase.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Human", id));

        if (!human.username().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        humanUseCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──

    record UpdateHumanRequest(
            @Size(max = 100) String displayName,
            @Email String email
    ) {}

    record HumanResponse(
            UUID id,
            String username,
            String email,
            String displayName,
            Instant createdAt,
            Instant lastLogin
    ) {
        static HumanResponse from(Human human) {
            return new HumanResponse(
                    human.id(),
                    human.username(),
                    human.email(),
                    human.displayName(),
                    human.createdAt(),
                    human.lastLogin()
            );
        }
    }
}
