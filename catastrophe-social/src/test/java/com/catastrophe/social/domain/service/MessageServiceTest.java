package com.catastrophe.social.domain.service;

import com.catastrophe.commons.exception.CatastropheExceptions.BusinessRuleViolationException;
import com.catastrophe.commons.exception.CatastropheExceptions.ResourceNotFoundException;
import com.catastrophe.social.domain.model.Message;
import com.catastrophe.social.domain.port.in.MessageUseCase.SendMessageCommand;
import com.catastrophe.social.domain.port.out.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de MessageService — envío, lectura, conversaciones.
 */
class MessageServiceTest {

    private MessageRepository messageRepository;
    private MessageService service;

    private final UUID senderId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        service = new MessageService(messageRepository);

        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Envío de mensajes")
    class Send {

        @Test
        @DisplayName("Mensaje válido se persiste con read=false")
        void validMessage() {
            var cmd = new SendMessageCommand(senderId, receiverId, "Hola, gato");

            var result = service.send(cmd);

            assertThat(result.senderId()).isEqualTo(senderId);
            assertThat(result.receiverId()).isEqualTo(receiverId);
            assertThat(result.content()).isEqualTo("Hola, gato");
            assertThat(result.read()).isFalse();
        }

        @Test
        @DisplayName("Self-message lanza BusinessRuleViolationException con código SELF_MESSAGE")
        void selfMessageThrows() {
            var cmd = new SendMessageCommand(senderId, senderId, "Hola yo mismo");

            assertThatThrownBy(() -> service.send(cmd))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("a sí mismo");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Contenido vacío lanza BusinessRuleViolationException")
        void rejectsEmptyContent() {
            var cmd = new SendMessageCommand(senderId, receiverId, "   ");

            assertThatThrownBy(() -> service.send(cmd))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("vacío");
        }

        @Test
        @DisplayName("Contenido null lanza BusinessRuleViolationException")
        void rejectsNullContent() {
            var cmd = new SendMessageCommand(senderId, receiverId, null);

            assertThatThrownBy(() -> service.send(cmd))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("Marcar como leído")
    class Reading {

        @Test
        @DisplayName("Receptor marca como leído: persiste con read=true")
        void receiverCanMarkAsRead() {
            var messageId = UUID.randomUUID();
            var msg = new Message(messageId, senderId, receiverId, "x", false, Instant.now());
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));

            service.markAsRead(messageId, receiverId);

            var captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().read()).isTrue();
        }

        @Test
        @DisplayName("Sender NO puede marcar su propio envío como leído")
        void senderCannotMarkAsRead() {
            var messageId = UUID.randomUUID();
            var msg = new Message(messageId, senderId, receiverId, "x", false, Instant.now());
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> service.markAsRead(messageId, senderId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("receptor");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tercero (ni sender ni receiver) tampoco puede marcar como leído")
        void thirdPartyCannotMarkAsRead() {
            var messageId = UUID.randomUUID();
            var thirdParty = UUID.randomUUID();
            var msg = new Message(messageId, senderId, receiverId, "x", false, Instant.now());
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> service.markAsRead(messageId, thirdParty))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("Mensaje inexistente lanza ResourceNotFoundException")
        void notFoundThrows() {
            var messageId = UUID.randomUUID();
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead(messageId, receiverId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("markConversationAsRead delega en el repo")
        void markConversationDelegates() {
            service.markConversationAsRead(receiverId, senderId);
            verify(messageRepository).markConversationAsRead(receiverId, senderId);
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Queries {

        @Test
        @DisplayName("getConversation delega con paginación")
        void getConversationDelegates() {
            when(messageRepository.findConversation(senderId, receiverId, 0, 50))
                    .thenReturn(List.of());

            service.getConversation(senderId, receiverId, 0, 50);

            verify(messageRepository).findConversation(senderId, receiverId, 0, 50);
        }

        @Test
        @DisplayName("getInbox delega")
        void getInboxDelegates() {
            when(messageRepository.findInbox(receiverId)).thenReturn(List.of());

            service.getInbox(receiverId);

            verify(messageRepository).findInbox(receiverId);
        }

        @Test
        @DisplayName("countUnread delega")
        void countUnreadDelegates() {
            when(messageRepository.countUnreadByCatId(receiverId)).thenReturn(7);

            assertThat(service.countUnread(receiverId)).isEqualTo(7);
        }
    }
}
