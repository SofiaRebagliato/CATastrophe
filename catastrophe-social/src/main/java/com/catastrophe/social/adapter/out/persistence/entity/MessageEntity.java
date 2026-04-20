package com.catastrophe.social.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MessageEntity() {}

    public MessageEntity(UUID id, UUID senderId, UUID receiverId,
                         String content, boolean read, Instant createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.read = read;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }

    public void setRead(boolean read) { this.read = read; }
}
