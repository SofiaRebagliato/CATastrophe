package com.catastrophe.social.adapter.in.web;

import com.catastrophe.social.domain.model.Follow;
import com.catastrophe.social.domain.port.in.FollowUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Seguimientos entre gatos (follow/unfollow).
 */
@RestController
@RequestMapping("/api/v1/follows")
public class FollowController {

    private final FollowUseCase followUseCase;

    public FollowController(FollowUseCase followUseCase) {
        this.followUseCase = followUseCase;
    }

    @PostMapping("/{followedId}")
    public ResponseEntity<FollowResponse> follow(
            @PathVariable UUID followedId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        var follow = followUseCase.follow(catId, followedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(FollowResponse.from(follow));
    }

    @DeleteMapping("/{followedId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID followedId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        followUseCase.unfollow(catId, followedId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{otherCatId}")
    public ResponseEntity<FollowStatusResponse> status(
            @PathVariable UUID otherCatId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        return ResponseEntity.ok(new FollowStatusResponse(
                followUseCase.isFollowing(catId, otherCatId)
        ));
    }

    @GetMapping("/following/{catId}")
    public ResponseEntity<List<FollowResponse>> getFollowing(@PathVariable UUID catId) {
        var follows = followUseCase.getFollowing(catId).stream()
                .map(FollowResponse::from).toList();
        return ResponseEntity.ok(follows);
    }

    @GetMapping("/followers/{catId}")
    public ResponseEntity<List<FollowResponse>> getFollowers(@PathVariable UUID catId) {
        var followers = followUseCase.getFollowers(catId).stream()
                .map(FollowResponse::from).toList();
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/count/{catId}")
    public ResponseEntity<FollowCountResponse> count(@PathVariable UUID catId) {
        return ResponseEntity.ok(new FollowCountResponse(
                followUseCase.countFollowers(catId),
                followUseCase.countFollowing(catId)
        ));
    }

    // ── DTOs ──

    record FollowResponse(UUID id, UUID followerId, UUID followedId, Instant createdAt) {
        static FollowResponse from(Follow f) {
            return new FollowResponse(f.id(), f.followerId(), f.followedId(), f.createdAt());
        }
    }

    record FollowStatusResponse(boolean following) {}

    record FollowCountResponse(int followers, int following) {}
}
