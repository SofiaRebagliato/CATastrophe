package com.catastrophe.social.adapter.in.web;

import com.catastrophe.social.domain.port.in.LikeUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Adaptador de entrada REST — Likes en publicaciones.
 */
@RestController
@RequestMapping("/api/v1/likes")
public class LikeController {

    private final LikeUseCase likeUseCase;

    public LikeController(LikeUseCase likeUseCase) {
        this.likeUseCase = likeUseCase;
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<LikeStatusResponse> like(
            @PathVariable UUID postId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        likeUseCase.like(postId, catId);
        return ResponseEntity.ok(new LikeStatusResponse(true, likeUseCase.countByPostId(postId)));
    }

    @DeleteMapping("/post/{postId}")
    public ResponseEntity<LikeStatusResponse> unlike(
            @PathVariable UUID postId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        likeUseCase.unlike(postId, catId);
        return ResponseEntity.ok(new LikeStatusResponse(false, likeUseCase.countByPostId(postId)));
    }

    @GetMapping("/post/{postId}/status")
    public ResponseEntity<LikeStatusResponse> status(
            @PathVariable UUID postId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        return ResponseEntity.ok(new LikeStatusResponse(
                likeUseCase.hasLiked(postId, catId),
                likeUseCase.countByPostId(postId)
        ));
    }

    record LikeStatusResponse(boolean liked, int likeCount) {}
}
