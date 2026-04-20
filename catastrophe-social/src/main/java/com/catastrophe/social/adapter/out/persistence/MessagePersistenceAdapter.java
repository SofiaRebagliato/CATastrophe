package com.catastrophe.social.adapter.out.persistence;

import com.catastrophe.social.adapter.out.persistence.mapper.MessageMapper;
import com.catastrophe.social.adapter.out.persistence.repository.JpaMessageRepository;
import com.catastrophe.social.domain.model.Message;
import com.catastrophe.social.domain.port.out.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MessagePersistenceAdapter implements MessageRepository {

    private final JpaMessageRepository jpaRepository;

    public MessagePersistenceAdapter(JpaMessageRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Message save(Message message) {
        var entity = MessageMapper.toEntity(message);
        return MessageMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return jpaRepository.findById(id).map(MessageMapper::toDomain);
    }

    @Override
    public List<Message> findConversation(UUID catId, UUID otherCatId, int page, int size) {
        return jpaRepository.findConversation(catId, otherCatId, PageRequest.of(page, size))
                .stream().map(MessageMapper::toDomain).toList();
    }

    @Override
    public List<Message> findInbox(UUID catId) {
        return jpaRepository.findInbox(catId).stream()
                .map(MessageMapper::toDomain).toList();
    }

    @Override
    public void markConversationAsRead(UUID catId, UUID otherCatId) {
        jpaRepository.markConversationAsRead(catId, otherCatId);
    }

    @Override
    public int countUnreadByCatId(UUID catId) {
        return jpaRepository.countUnreadByReceiverId(catId);
    }
}
