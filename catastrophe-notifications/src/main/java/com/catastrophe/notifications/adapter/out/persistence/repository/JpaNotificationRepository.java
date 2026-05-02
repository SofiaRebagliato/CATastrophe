package com.catastrophe.notifications.adapter.out.persistence.repository;

import com.catastrophe.notifications.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    boolean existsByEventId(UUID eventId);

    List<NotificationEntity> findByRecipientCatIdOrderByCreatedAtDesc(UUID recipientCatId, Pageable pageable);

    List<NotificationEntity> findByRecipientCatIdAndReadFalseOrderByCreatedAtDesc(UUID recipientCatId, Pageable pageable);

    long countByRecipientCatIdAndReadFalse(UUID recipientCatId);

    @Modifying
    @Query("""
            UPDATE NotificationEntity n
            SET n.read = true, n.readAt = :readAt
            WHERE n.recipientCatId = :recipientCatId AND n.read = false
            """)
    int markAllAsRead(@Param("recipientCatId") UUID recipientCatId, @Param("readAt") Instant readAt);
}
