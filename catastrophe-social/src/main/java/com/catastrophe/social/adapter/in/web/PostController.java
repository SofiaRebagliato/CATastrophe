package com.catastrophe.social.adapter.in.web;

import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Post;
import com.catastrophe.social.domain.port.in.PostUseCase;
import com.catastrophe.social.domain.port.in.PostUseCase.CreatePostCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Publicaciones (Meows).
 *
 * Nota: El catId se recibe como header "X-Cat-Id" inyectado por el
 * Gateway o extraído de la sesión del humano autenticado.
 * En F3 se usa header directo; en F6 se integrará con la sesión.
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostUseCase postUseCase;

    public PostController(PostUseCase postUseCase) {
        this.postUseCase = postUseCase;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody CreatePostRequest request,
            @RequestHeader("X-Cat-Id") UUID catId) {

        var command = new CreatePostCommand(
                catId,
                request.content(),
                request.imageUrl(),
                request.postType()
        );
        var post = postUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(post));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> findById(@PathVariable UUID id) {
        return postUseCase.findById(id)
                .map(post -> ResponseEntity.ok(PostResponse.from(post)))
                .orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    @GetMapping("/cat/{catId}")
    public ResponseEntity<List<PostResponse>> findByCat(
            @PathVariable UUID catId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var posts = postUseCase.findByCatId(catId, page, size).stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeed(
            @RequestHeader("X-Cat-Id") UUID catId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var posts = postUseCase.getFeed(catId, page, size).stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(posts);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-Cat-Id") UUID catId) {
        postUseCase.delete(id, catId);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──

    record CreatePostRequest(
            @NotBlank(message = "El contenido del meow es obligatorio")
            @Size(max = 500, message = "Un meow no puede superar los 500 caracteres")
            String content,

            @Size(max = 500) String imageUrl,
            String postType
    ) {}

    record PostResponse(
            UUID id,
            UUID catId,
            String content,
            String imageUrl,
            String postType,
            int likeCount,
            int commentCount,
            Instant createdAt
    ) {
        static PostResponse from(Post post) {
            return new PostResponse(
                    post.id(), post.catId(), post.content(), post.imageUrl(),
                    post.postType(), post.likeCount(), post.commentCount(), post.createdAt()
            );
        }
    }
}
