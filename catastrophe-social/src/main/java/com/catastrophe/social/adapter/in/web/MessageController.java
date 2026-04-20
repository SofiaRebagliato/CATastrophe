package com.catastrophe.social.adapter.in.web;

import com.catastrophe.social.domain.model.Message;
import com.catastrophe.social.domain.port.in.MessageUseCase;
import com.catastrophe.social.domain.port.in.MessageUseCase.SendMessageCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST — Mensajería privada entre gatos.
 *
 * Endpoints:
 *  - POST   /api/v1/messages           → Enviar mensaje
 *  - GET    /api/v1/messages/inbox      → Bandeja de entrada
 *  - GET    /api/v1/messages/with/{id}  → Conversación con un gato
 *  - PUT    /api/v1/messages/{id}/read  → Marcar mensaje como leído
 *  - PUT    /api/v1/messages/read/{id}  → Marcar conversación como leída
 *  - GET    /api/v1/messages/unread     → Contador de no leídos
 */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageUseCase messageUseCase;

    public MessageController(MessageUseCase messageUseCase) {
        this.messageUseCase = messageUseCase;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader("X-Cat-Id") UUID catId) {

        var command = new SendMessageCommand(catId, request.receiverId(), request.content());
        var message = messageUseCase.send(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message));
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageResponse>> inbox(
            @RequestHeader("X-Cat-Id") UUID catId) {
        var messages = messageUseCase.getInbox(catId).stream()
                .map(MessageResponse::from).toList();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/with/{otherCatId}")
    public ResponseEntity<List<MessageResponse>> conversation(
            @PathVariable UUID otherCatId,
            @RequestHeader("X-Cat-Id") UUID catId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var messages = messageUseCase.getConversation(catId, otherCatId, page, size).stream()
                .map(MessageResponse::from).toList();
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID messageId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        messageUseCase.markAsRead(messageId, catId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read/{otherCatId}")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable UUID otherCatId,
            @RequestHeader("X-Cat-Id") UUID catId) {
        messageUseCase.markConversationAsRead(catId, otherCatId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread")
    public ResponseEntity<UnreadCountResponse> unreadCount(
            @RequestHeader("X-Cat-Id") UUID catId) {
        return ResponseEntity.ok(new UnreadCountResponse(messageUseCase.countUnread(catId)));
    }

    // ── DTOs ──

    record SendMessageRequest(
            UUID receiverId,
            @NotBlank(message = "El mensaje no puede estar vacío")
            @Size(max = 1000, message = "Un mensaje no puede superar los 1000 caracteres")
            String content
    ) {}

    record MessageResponse(
            UUID id, UUID senderId, UUID receiverId,
            String content, boolean read, Instant createdAt
    ) {
        static MessageResponse from(Message m) {
            return new MessageResponse(
                    m.id(), m.senderId(), m.receiverId(),
                    m.content(), m.read(), m.createdAt()
            );
        }
    }

    record UnreadCountResponse(int unreadCount) {}
}
