package com.catastrophe.social.adapter.out.persistence.mapper;

import com.catastrophe.social.adapter.out.persistence.entity.CommentEntity;
import com.catastrophe.social.domain.model.Comment;

public final class CommentMapper {

    private CommentMapper() {}

    public static Comment toDomain(CommentEntity entity) {
        return new Comment(
                entity.getId(),
                entity.getPostId(),
                entity.getCatId(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    public static CommentEntity toEntity(Comment comment) {
        return new CommentEntity(
                comment.id(),
                comment.postId(),
                comment.catId(),
                comment.content(),
                comment.createdAt()
        );
    }
}
