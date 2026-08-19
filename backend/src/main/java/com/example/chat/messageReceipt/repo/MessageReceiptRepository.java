package com.example.chat.messageReceipt.repo;

import com.example.chat.dto.DeliveryEventDTO;
import com.example.chat.messageReceipt.entity.MessageReceipt;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface MessageReceiptRepository
        extends JpaRepository<MessageReceipt, Long> {

    MessageReceipt save(MessageReceipt receipt);

    @Query("""
SELECT r FROM MessageReceipt r
WHERE r.message.id IN :messageIds
AND r.userId = :userId
""")
    List<MessageReceipt> findByMessageIdsAndUserId(List<Long> messageIds, Long userId);

    @Modifying
    @Query("""
    UPDATE MessageReceipt mr
    SET mr.delivered = true
    WHERE mr.userId = :userId
    AND mr.delivered = false
    AND mr.message.conversation.id = :conversationId
    AND mr.message.createdAt >= :joinedAt
""")
    void markDeliveredWithinWindow(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("joinedAt") LocalDateTime joinedAt
    );

    @Modifying
    @Query("""
    UPDATE MessageReceipt mr
    SET mr.seen = true,
        mr.delivered = true
    WHERE mr.userId = :userId
    AND mr.seen = false
    AND mr.message.conversation.id = :conversationId
    AND mr.message.createdAt >= :joinedAt
""")
    void markSeenWithinWindow(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("joinedAt") LocalDateTime joinedAt
    );

    Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId, Long userId);

    @Query("""
    SELECT COUNT(r)
    FROM MessageReceipt r
    JOIN r.message m
    WHERE m.conversation.id = :conversationId
      AND r.userId = :userId
      AND r.seen = false
      AND r.deletedForMe = false
      AND m.createdAt >= :joinedAt
      AND m.senderId <> :userId
      AND m.deletedForEveryone = false
""")
    long countUnread(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("joinedAt") LocalDateTime joinedAt
    );

    List<MessageReceipt> findByMessageId(Long messageId);

    @Query("""
    SELECT new com.example.chat.dto.DeliveryEventDTO(
        mr.message.id,
        mr.message.conversation.id,
        mr.message.senderId
    )
    FROM MessageReceipt mr,
         ParticipantLifecycle pl
    WHERE mr.userId = :userId
      AND mr.delivered = false
      AND mr.message.senderId <> :userId
      AND pl.userId = :userId
      AND pl.conversationId = mr.message.conversation.id
      AND pl.leftAt IS NULL
      AND mr.message.createdAt >= pl.joinedAt
""")
    List<DeliveryEventDTO> findPendingDeliveryEvents(
            @Param("userId") Long userId
    );

    @Modifying
    @Query("""
    UPDATE MessageReceipt mr
    SET mr.delivered = true,
        mr.deliveredAt = :deliveredAt
    WHERE mr.userId = :userId
      AND mr.delivered = false
      AND mr.message.senderId <> :userId
      AND EXISTS (
            SELECT pl.id
            FROM ParticipantLifecycle pl
            WHERE pl.userId = :userId
              AND pl.conversationId = mr.message.conversation.id
              AND pl.leftAt IS NULL
              AND mr.message.createdAt >= pl.joinedAt
      )
""")
    int bulkMarkDelivered(
            @Param("userId") Long userId,
            @Param("deliveredAt") LocalDateTime deliveredAt
    );

    @Query("""
    SELECT new com.example.chat.dto.DeliveryEventDTO(
        mr.message.id,
        mr.message.conversation.id,
        mr.message.senderId
    )
    FROM MessageReceipt mr,
         com.example.chat.participantLifecycle.entity.ParticipantLifecycle pl
    WHERE mr.userId = :userId
      AND mr.seen = false
      AND mr.message.senderId <> :userId
      AND mr.message.conversation.id = :conversationId
      AND pl.userId = :userId
      AND pl.conversationId = mr.message.conversation.id
      AND pl.leftAt IS NULL
      AND mr.message.createdAt >= pl.joinedAt
""")
    List<DeliveryEventDTO> findPendingSeenEvents(
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId
    );

    @Modifying
    @Query("""
    UPDATE MessageReceipt mr
    SET mr.seen = true,
        mr.seenAt = :seenAt,
        mr.delivered = true,
        mr.deliveredAt = COALESCE(mr.deliveredAt, :seenAt)
    WHERE mr.userId = :userId
      AND mr.seen = false
      AND mr.message.senderId <> :userId
      AND mr.message.conversation.id = :conversationId
      AND EXISTS (
            SELECT pl.id
            FROM com.example.chat.participantLifecycle.entity.ParticipantLifecycle pl
            WHERE pl.userId = :userId
              AND pl.conversationId = mr.message.conversation.id
              AND pl.leftAt IS NULL
              AND mr.message.createdAt >= pl.joinedAt
      )
""")
    int bulkMarkSeen(
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId,
            @Param("seenAt") LocalDateTime seenAt
    );
}