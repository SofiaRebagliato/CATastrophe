package com.catastrophe.social.adapter.in.web;

import com.catastrophe.social.domain.model.Comment;
import com.catastrophe.social.domain.port.in.CommentUseCase;
import com.catastrophe.social.domain.port.in.CommentUseCase.CreateCommentCommand;
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
 * Adaptador de entrada REST — Comentarios en publicaciones.
 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentUseCase commentUseCase;

    public CommentController(CommentUseCase commentUseCase) {
        this.commentUseCase = commentUseCase;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader("X-Cat-Id") UUID catId) {

        var command = new CreateCommentCommand(request.postId(), catId, request.content());
        var comment = commentUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponse>> findByPost(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var comments = commentUseCase.findByPostId(postId, page, size).stream()
                .map(CommentResponse::from)
                .toList();
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-Cat-Id") UUID catId) {
        commentUseCase.delete(id, catId);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──

    record CreateCommentRequest(
            UUID postId,
            @NotBlank(message = "El comentario no puede estar vacío")
            @Size(max = 300, message = "Un comentario no puede superar los 300 caracteres")
            String content
    ) {}

    record CommentResponse(UUID id, UUID postId, UUID catId, String content, Instant createdAt) {
        static CommentResponse from(Comment c) {
            return new CommentResponse(c.id(), c.postId(), c.catId(), c.content(), c.createdAt());
        }
    }
}
