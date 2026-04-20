package com.catastrophe.social.domain.port.in;

import com.catastrophe.social.domain.model.Message;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada — Casos de uso de mensajería privada entre gatos.
 */
public interface MessageUseCase {

    /** Enviar un mensaje privado. */
    Message send(SendMessageCommand command);

    /** Obtener conversación entre dos gatos, paginada. */
    List<Message> getConversation(UUID catId, UUID otherCatId, int page, int size);

    /** Obtener bandeja de entrada (últimos mensajes de cada conversación). */
    List<Message> getInbox(UUID catId);

    /** Marcar un mensaje como leído. */
    void markAsRead(UUID messageId, UUID catId);

    /** Marcar todos los mensajes de una conversación como leídos. */
    void markConversationAsRead(UUID catId, UUID otherCatId);

    /** Contar mensajes no leídos. */
    int countUnread(UUID catId);

    record SendMessageCommand(
            UUID senderId,
            UUID receiverId,
            String content
    ) {}
}
