package com.catastrophe.social.domain.service;

import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Message;
import com.catastrophe.social.domain.port.in.MessageUseCase;
import com.catastrophe.social.domain.port.out.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de dominio — Lógica de negocio de mensajería privada.
 *
 * Un gato no puede enviarse mensajes a sí mismo.
 * Solo el receptor puede marcar mensajes como leídos.
 */
@Service
@Transactional
public class MessageService implements MessageUseCase {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Message send(SendMessageCommand command) {
        if (command.content() == null || command.content().isBlank()) {
            throw new BusinessRuleViolationException(
                    "MESSAGE_CONTENT_REQUIRED",
                    "No puedes enviar un mensaje vacío. ¡Los gatos tienen mucho que decir!"
            );
        }

        if (command.senderId().equals(command.receiverId())) {
            throw new BusinessRuleViolationException(
                    "SELF_MESSAGE",
                    "¿Hablando solo? ¡Un gato no puede enviarse mensajes a sí mismo!"
            );
        }

        var message = Message.create(command.senderId(), command.receiverId(), command.content());
        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversation(UUID catId, UUID otherCatId, int page, int size) {
        return messageRepository.findConversation(catId, otherCatId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getInbox(UUID catId) {
        return messageRepository.findInbox(catId);
    }

    @Override
    public void markAsRead(UUID messageId, UUID catId) {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId));

        // Solo el receptor puede marcar como leído
        if (!message.receiverId().equals(catId)) {
            throw new BusinessRuleViolationException(
                    "MESSAGE_READ_OWNERSHIP",
                    "Solo el receptor puede marcar un mensaje como leído."
            );
        }

        messageRepository.save(message.markAsRead());
    }

    @Override
    public void markConversationAsRead(UUID catId, UUID otherCatId) {
        messageRepository.markConversationAsRead(catId, otherCatId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countUnread(UUID catId) {
        return messageRepository.countUnreadByCatId(catId);
    }
}
