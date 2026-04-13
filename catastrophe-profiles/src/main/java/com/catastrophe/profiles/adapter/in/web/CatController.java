package com.catastrophe.profiles.adapter.in.web;

import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.profiles.domain.model.Cat;
import com.catastrophe.profiles.domain.port.in.CatUseCase;
import com.catastrophe.profiles.domain.port.in.CatUseCase.CreateCatCommand;
import com.catastrophe.profiles.domain.port.in.CatUseCase.UpdateCatCommand;
import com.catastrophe.profiles.domain.port.in.HumanUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Expone los casos de uso de gatos como API HTTP.
 *
 * En Fase 2 se vincula con el usuario autenticado:
 *  - Crear gato: se asocia al humano de la sesión actual
 *  - Editar/borrar: solo el dueño puede modificar sus gatos
 *  - Ver: público (sin autenticación necesaria)
 */
@RestController
@RequestMapping("/api/v1/cats")
public class CatController {

    private final CatUseCase catUseCase;
    private final HumanUseCase humanUseCase;

    public CatController(CatUseCase catUseCase, HumanUseCase humanUseCase) {
        this.catUseCase = catUseCase;
        this.humanUseCase = humanUseCase;
    }

    @PostMapping
    public ResponseEntity<CatResponse> create(@Valid @RequestBody CreateCatRequest request,
                                               Authentication authentication) {
        // Obtener el humano autenticado
        var human = humanUseCase.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Human", null));

        var command = new CreateCatCommand(
                human.id(),
                request.name(),
                request.breed(),
                request.ageMonths(),
                request.bio()
        );
        var cat = catUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CatResponse.from(cat));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatResponse> findById(@PathVariable UUID id) {
        return catUseCase.findById(id)
                .map(cat -> ResponseEntity.ok(CatResponse.from(cat)))
                .orElseThrow(() -> new ResourceNotFoundException("Cat", id));
    }

    @GetMapping("/human/{humanId}")
    public ResponseEntity<List<CatResponse>> findByHuman(@PathVariable UUID humanId) {
        var cats = catUseCase.findByHumanId(humanId).stream()
                .map(CatResponse::from)
                .toList();
        return ResponseEntity.ok(cats);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CatResponse>> findMyCats(Authentication authentication) {
        var human = humanUseCase.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Human", null));

        var cats = catUseCase.findByHumanId(human.id()).stream()
                .map(CatResponse::from)
                .toList();
        return ResponseEntity.ok(cats);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateCatRequest request,
                                               Authentication authentication) {
        // Verificar que el gato pertenece al humano autenticado
        verifyOwnership(id, authentication);

        var command = new UpdateCatCommand(
                request.name(),
                request.breed(),
                request.ageMonths(),
                request.bio()
        );
        var cat = catUseCase.update(id, command);
        return ResponseEntity.ok(CatResponse.from(cat));
    }

    @PostMapping("/{id}/refresh-avatar")
    public ResponseEntity<CatResponse> refreshAvatar(@PathVariable UUID id,
                                                      Authentication authentication) {
        verifyOwnership(id, authentication);
        var cat = catUseCase.refreshAvatar(id);
        return ResponseEntity.ok(CatResponse.from(cat));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                        Authentication authentication) {
        verifyOwnership(id, authentication);
        catUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Verificación de propiedad ──

    private void verifyOwnership(UUID catId, Authentication authentication) {
        var cat = catUseCase.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Cat", catId));
        var human = humanUseCase.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Human", null));

        if (!cat.humanId().equals(human.id())) {
            throw new com.catastrophe.commons.exception.CatastropheExceptions
                    .BusinessRuleViolationException(
                    "CAT_OWNERSHIP",
                    "Este gato no te pertenece. ¡Solo su humano asistente puede gestionarlo!"
            );
        }
    }

    // ── Request/Response DTOs ──

    record CreateCatRequest(
            @NotBlank(message = "El nombre del gato es obligatorio")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
            String name,

            @Size(max = 100, message = "La raza no puede superar los 100 caracteres")
            String breed,

            Integer ageMonths,

            String bio
    ) {}

    record UpdateCatRequest(
            @Size(max = 100) String name,
            @Size(max = 100) String breed,
            Integer ageMonths,
            String bio
    ) {}

    record CatResponse(
            UUID id,
            UUID humanId,
            String name,
            String breed,
            Integer ageMonths,
            String avatarUrl,
            String bio,
            int xp,
            int level,
            String mood
    ) {
        static CatResponse from(Cat cat) {
            return new CatResponse(
                    cat.id(), cat.humanId(), cat.name(), cat.breed(),
                    cat.ageMonths(), cat.avatarUrl(), cat.bio(),
                    cat.xp(), cat.level(), cat.mood()
            );
        }
    }
}
