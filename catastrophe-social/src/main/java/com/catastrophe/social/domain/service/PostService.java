package com.catastrophe.social.domain.service;

import com.catastrophe.commons.event.CatastropheEvent.MeowPosted;
import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Post;
import com.catastrophe.social.domain.port.in.PostUseCase;
import com.catastrophe.social.domain.port.out.EventPublisher;
import com.catastrophe.social.domain.port.out.FollowRepository;
import com.catastrophe.social.domain.port.out.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de publicaciones.
 *
 * El feed se construye obteniendo los IDs de los gatos seguidos
 * y consultando sus posts ordenados cronológicamente.
 */
@Service
@Transactional
public class PostService implements PostUseCase {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final EventPublisher eventPublisher;

    public PostService(PostRepository postRepository,
                       FollowRepository followRepository,
                       EventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.followRepository = followRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Post create(CreatePostCommand command) {
        if (command.content() == null || command.content().isBlank()) {
            throw new BusinessRuleViolationException(
                    "POST_CONTENT_REQUIRED",
                    "Un meow sin contenido es como un gato sin bigotes. ¡Escribe algo!"
            );
        }

        var post = Post.create(
                command.catId(),
                command.content(),
                command.imageUrl(),
                command.postType()
        );

        var saved = postRepository.save(post);

        // Publicar evento al bus de Kafka
        eventPublisher.publish(new MeowPosted(
                UUID.randomUUID(),
                Instant.now(),
                saved.catId(),
                saved.id(),
                saved.postType()
        ));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(UUID id) {
        return postRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findByCatId(UUID catId, int page, int size) {
        return postRepository.findByCatId(catId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getFeed(UUID catId, int page, int size) {
        // Obtener IDs de los gatos seguidos
        var followedIds = followRepository.findFollowedIds(catId);

        if (followedIds.isEmpty()) {
            return List.of(); // Sin seguidos, feed vacío
        }

        return postRepository.findByCatIds(followedIds, page, size);
    }

    @Override
    public void delete(UUID postId, UUID catId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (!post.catId().equals(catId)) {
            throw new BusinessRuleViolationException(
                    "POST_OWNERSHIP",
                    "¡Ese meow no es tuyo! Solo puedes borrar tus propios meows."
            );
        }

        postRepository.deleteById(postId);
    }
}
