package com.catastrophe.profiles.adapter.in.web;

import com.catastrophe.profiles.domain.model.Human;
import com.catastrophe.profiles.domain.port.in.HumanUseCase;
import com.catastrophe.profiles.domain.port.in.HumanUseCase.RegisterHumanCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Autenticación y registro de humanos.
 *
 * Endpoints públicos (no requieren autenticación):
 *  - POST /api/v1/auth/register → Registro de nuevo humano
 *  - POST /api/v1/auth/login    → Login con username + password
 *  - POST /api/v1/auth/logout   → Cerrar sesión
 *  - GET  /api/v1/auth/me       → Info del usuario autenticado
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final HumanUseCase humanUseCase;
    private final AuthenticationManager authenticationManager;

    public AuthController(HumanUseCase humanUseCase,
                          AuthenticationManager authenticationManager) {
        this.humanUseCase = humanUseCase;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        var command = new RegisterHumanCommand(
                request.username(),
                request.email(),
                request.password(),
                request.displayName()
        );

        var human = humanUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.from(human, "Registro exitoso. ¡Bienvenido, asistente de gatos!"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        // Autenticar con Spring Security
        var authToken = new UsernamePasswordAuthenticationToken(
                request.username(), request.password());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Establecer el contexto de seguridad
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Guardar en la sesión HTTP (que se persiste en Redis)
        var session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);

        // Actualizar último login
        var human = humanUseCase.findByUsername(request.username())
                .orElseThrow(); // No debería fallar si acaba de autenticarse

        return ResponseEntity.ok(
                AuthResponse.from(human, "Login exitoso. Tu gato te estaba esperando."));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Sesión cerrada. Tus gatos te echarán de menos."));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        return humanUseCase.findByUsername(username)
                .map(human -> ResponseEntity.ok(AuthResponse.from(human, null)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ── Request / Response DTOs ──

    record RegisterRequest(
            @NotBlank(message = "El username es obligatorio")
            @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
            String username,

            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El email debe ser válido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
            String password,

            @Size(max = 100, message = "El nombre visible no puede superar los 100 caracteres")
            String displayName
    ) {}

    record LoginRequest(
            @NotBlank(message = "El username es obligatorio")
            String username,

            @NotBlank(message = "La contraseña es obligatoria")
            String password
    ) {}

    record AuthResponse(
            UUID id,
            String username,
            String email,
            String displayName,
            Instant createdAt,
            Instant lastLogin,
            String message
    ) {
        static AuthResponse from(Human human, String message) {
            return new AuthResponse(
                    human.id(),
                    human.username(),
                    human.email(),
                    human.displayName(),
                    human.createdAt(),
                    human.lastLogin(),
                    message
            );
        }
    }

    record MessageResponse(String message) {}
}
