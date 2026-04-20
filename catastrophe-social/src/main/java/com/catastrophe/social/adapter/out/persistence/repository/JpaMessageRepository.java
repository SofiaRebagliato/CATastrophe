package com.catastrophe.social.adapter.out.persistence.repository;

import com.catastrophe.social.adapter.out.persistence.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaMessageRepository extends JpaRepository<MessageEntity, UUID> {

    /**
     * Obtener mensajes de una conversación entre dos gatos (ambas direcciones).
     */
    @Query("""
            SELECT m FROM MessageEntity m
            WHERE (m.senderId = :catId AND m.receiverId = :otherCatId)
               OR (m.senderId = :otherCatId AND m.receiverId = :catId)
            ORDER BY m.createdAt ASC
            """)
    List<MessageEntity> findConversation(
            @Param("catId") UUID catId,
            @Param("otherCatId") UUID otherCatId,
            Pageable pageable);

    /**
     * Bandeja de entrada: último mensaje de cada conversación donde el gato participa.
     * Usa una subquery para obtener el mensaje más reciente por contacto.
     */
    @Query(value = """
            SELECT DISTINCT ON (contact_id) m.*
            FROM (
                SELECT *, sender_id AS contact_id FROM messages WHERE receiver_id = :catId
                UNION ALL
                SELECT *, receiver_id AS contact_id FROM messages WHERE sender_id = :catId
            ) m
            ORDER BY contact_id, m.created_at DESC
            """, nativeQuery = true)
    List<MessageEntity> findInbox(@Param("catId") UUID catId);

    /**
     * Marcar como leídos todos los mensajes enviados por otherCatId a catId.
     */
    @Modifying
    @Query("""
            UPDATE MessageEntity m SET m.read = true
            WHERE m.receiverId = :catId AND m.senderId = :otherCatId AND m.read = false
            """)
    void markConversationAsRead(
            @Param("catId") UUID catId,
            @Param("otherCatId") UUID otherCatId);

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.receiverId = :catId AND m.read = false")
    int countUnreadByReceiverId(@Param("catId") UUID catId);
}
