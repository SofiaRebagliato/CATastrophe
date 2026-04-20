package com.catastrophe.social.adapter.out.persistence.mapper;

import com.catastrophe.social.adapter.out.persistence.entity.MessageEntity;
import com.catastrophe.social.domain.model.Message;

public final class MessageMapper {

    private MessageMapper() {}

    public static Message toDomain(MessageEntity entity) {
        return new Message(
                entity.getId(), entity.getSenderId(), entity.getReceiverId(),
                entity.getContent(), entity.isRead(), entity.getCreatedAt()
        );
    }

    public static MessageEntity toEntity(Message message) {
        return new MessageEntity(
                message.id(), message.senderId(), message.receiverId(),
                message.content(), message.read(), message.createdAt()
        );
    }
}
