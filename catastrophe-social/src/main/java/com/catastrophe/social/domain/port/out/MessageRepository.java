package com.catastrophe.social.domain.port.out;

import com.catastrophe.social.domain.model.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida — Persistencia de mensajes privados.
 */
public interface MessageRepository {

    Message save(Message message);

    Optional<Message> findById(UUID id);

    /** Conversación entre dos gatos, ordenada por fecha asc. */
    List<Message> findConversation(UUID catId, UUID otherCatId, int page, int size);

    /** Último mensaje de cada conversación del gato (bandeja de entrada). */
    List<Message> findInbox(UUID catId);

    /** Marcar como leídos todos los mensajes de otherCatId hacia catId. */
    void markConversationAsRead(UUID catId, UUID otherCatId);

    int countUnreadByCatId(UUID catId);
}
