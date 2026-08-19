package com.example.chat.messageReceipt.service;

import com.example.chat.dto.DeliveryEventDTO;
import com.example.chat.message.entity.ChatMessage;
import com.example.chat.messageReceipt.entity.MessageReceipt;
import com.example.chat.messageReceipt.repo.MessageReceiptRepository;
import com.example.websocket.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReceiptService {

    private final MessageReceiptRepository messageReceiptRepository;
    private final PresenceService presenceService;

    @Transactional
    public void createInitialReceipts(ChatMessage message,
                                      Long senderId,String senderHandleName,
                                      Long receiverId) {

        log.debug("[RECEIPT_INIT_PRIVATE] messageId={} sender={} receiver={}",
                message.getId(), senderId, receiverId);

        createReceipt(message, senderHandleName,senderId, true, true);

        log.info("[SENDER_RECEIPT_MARK_DELIVERED] senderId={}",senderId);

        boolean online =
                presenceService.isOnline(receiverId);
        createReceipt(message, senderHandleName,receiverId, online, false);

        if(online){
            log.info("[RECEIVER_RECEIPT_MARK_DELIVERED] receiverId={}",receiverId);
        }

        log.info("[RECEIPTS_CREATED_PRIVATE] messageId={}", message.getId());
    }

    private void createReceipt(ChatMessage message,
                                     String handleName,
                                     Long userId,
                                     boolean delivered,
                                     boolean seen) {
        try {


            MessageReceipt receipt = MessageReceipt.builder()
                    .message(message)
                    .userId(userId)
                    .delivered(delivered)
                    .deliveredAt(delivered?LocalDateTime.now():null)
                    .seen(seen)
                    .seenAt(seen ? LocalDateTime.now() : null)
                    .deletedForMe(false)
                    .handleName(handleName)
                    .build();

            messageReceiptRepository.save(receipt);

            log.debug("[RECEIPT_CREATED] messageId={} userId={}",
                    message.getId(), userId);

        } catch (DataIntegrityViolationException ex) {
            // Already created by another tx / retry
            log.debug("[RECEIPT_ALREADY_EXISTS_RACE_SAFE] messageId={} userId={}",
                    message.getId(), userId);
        }
    }

    public List<MessageReceipt> getReceiptsForMessageIds(List<Long> messageIds, Long userId){

        log.debug("[FETCH_RECEIPTS] userId={} messageCount={}", userId, messageIds.size());

        try {
            log.info("[FETCH_RECEIPTS_SUCCESS] userId={} messageCount={}",userId,messageIds.size());
            return messageReceiptRepository
                    .findByMessageIdsAndUserId(messageIds, userId);
        } catch (Exception ex) {
            log.error("[DB_RECEIPT_FETCH_FAILED] userId={}", userId, ex);
            throw ex;
        }
    }


    @Transactional
    public void markAllDelivered(Long userId, Map<Long, LocalDateTime> conversationJoinedAtMap) {

        log.info("[RECEIPT_MARK_ALL_DELIVERED] userId={} conversations={}",
                userId, conversationJoinedAtMap.keySet());

        try {
            conversationJoinedAtMap.forEach((conversationId, joinedAt) -> {
                messageReceiptRepository
                        .markDeliveredWithinWindow(conversationId, userId, joinedAt);
            });

            log.info("[RECEIPT_MARK_ALL_DELIVERED_SUCCESS] userId={}", userId);

        } catch (Exception ex) {
            log.error("[RECEIPT_MARK_ALL_DELIVERED_FAILED] userId={} error={}",
                    userId, ex.getMessage(), ex);
            throw ex; // IMPORTANT → triggers rollback
        }
    }

    @Transactional
    public void markAllSeen(Long conversationId, Long userId, LocalDateTime joinedAt) {

        log.info("[RECEIPT_MARK_ALL_SEEN] convoId={} userId={}", conversationId, userId);

        try {
            messageReceiptRepository
                    .markSeenWithinWindow(conversationId, userId, joinedAt);

            log.info("[RECEIPT_MARK_ALL_SEEN_SUCCESS] convoId={} userId={}", conversationId, userId);

        } catch (Exception ex) {
            log.error("[RECEIPT_MARK_ALL_SEEN_FAILED] convoId={} userId={} error={}",
                    conversationId, userId, ex.getMessage(), ex);
            throw ex; // rollback
        }
    }

    public MessageReceipt getReceiptByMessageAndUser(Long messageId, Long userId) {
        try {
            log.info("[GET_RECEIPT_REQUEST] messageId={}, userId={}", messageId, userId);

            MessageReceipt receipt = messageReceiptRepository
                    .findByMessageIdAndUserId(messageId, userId)
                    .orElseThrow(() -> {
                        log.warn("[RECEIPT_NOT_FOUND] messageId={}, userId={}", messageId, userId);
                        return new RuntimeException(
                                "Receipt not found for messageId=" + messageId + ", userId=" + userId
                        );
                    });

            log.info("[GET_RECEIPT_SUCCESS] messageId={}, userId={}", messageId, userId);
            return receipt;

        } catch (RuntimeException e) {
            // expected business case → log + rethrow
            log.error("[GET_RECEIPT_BUSINESS_FAIL] messageId={}, userId={}",
                    messageId, userId, e);
            throw e;

        } catch (Exception e) {
            // unexpected system failure
            log.error("[GET_RECEIPT_SYSTEM_FAIL] messageId={}, userId={}",
                    messageId, userId, e);
            throw new RuntimeException("Internal error while fetching receipt", e);
        }
    }

    @Transactional
    public void markDeletedForMe(MessageReceipt receipt) {

        try {
            log.debug("[MARK_DELETE_FOR_ME_INTERNAL] receiptId={}, currentState={}",
                    receipt.getId(), receipt.isDeletedForMe());

            receipt.setDeletedForMe(true);

            messageReceiptRepository.save(receipt);

            log.info("[MARK_DELETE_FOR_ME_INTERNAL_SUCCESS] receiptId={}",
                    receipt.getId());

        } catch (Exception e) {
            log.error("[MARK_DELETE_FOR_ME_INTERNAL_FAILED] receiptId={}",
                    receipt.getId(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public long countUnread(
            Long conversationId,
            Long userId,
            LocalDateTime joinedAt
    ) {

        log.info(
                "[COUNT_UNREAD] convoId={} userId={}",
                conversationId,
                userId
        );

        try {

            long count = messageReceiptRepository.countUnread(
                    conversationId,
                    userId,
                    joinedAt
            );

            log.info(
                    "[COUNT_UNREAD_SUCCESS] convoId={} userId={} count={}",
                    conversationId,
                    userId,
                    count
            );

            return count;

        } catch (Exception ex) {

            log.error(
                    "[COUNT_UNREAD_ERROR] convoId={} userId={}",
                    conversationId,
                    userId,
                    ex
            );

            throw ex;
        }
    }

    public List<MessageReceipt> findByMessageId(Long messageId) {
        return messageReceiptRepository.findByMessageId(messageId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryEventDTO> findPendingDeliveryEvents(Long userId) {

        log.info(
                "[FIND_PENDING_DELIVERY_EVENTS] userId={}",
                userId
        );

        try {

            List<DeliveryEventDTO> events =
                    messageReceiptRepository
                            .findPendingDeliveryEvents(userId);

            log.info(
                    "[FIND_PENDING_DELIVERY_EVENTS_SUCCESS] userId={} count={}",
                    userId,
                    events.size()
            );

            return events;

        } catch (Exception ex) {

            log.error(
                    "[FIND_PENDING_DELIVERY_EVENTS_FAILED] userId={} error={}",
                    userId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    @Transactional
    public int bulkMarkDelivered(
            Long userId,
            LocalDateTime deliveredAt
    ) {

        log.info(
                "[BULK_MARK_DELIVERED] userId={} deliveredAt={}",
                userId,
                deliveredAt
        );

        try {

            int updatedRows =
                    messageReceiptRepository
                            .bulkMarkDelivered(
                                    userId,
                                    deliveredAt
                            );

            log.info(
                    "[BULK_MARK_DELIVERED_SUCCESS] userId={} updatedRows={}",
                    userId,
                    updatedRows
            );

            return updatedRows;

        } catch (Exception ex) {

            log.error(
                    "[BULK_MARK_DELIVERED_FAILED] userId={} error={}",
                    userId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DeliveryEventDTO> findPendingSeenEvents(
            Long userId,
            Long conversationId
    ) {
        log.info(
                "[FIND_PENDING_SEEN_EVENTS] userId={} conversationId={}",
                userId,
                conversationId
        );

        try {
            List<DeliveryEventDTO> events =
                    messageReceiptRepository.findPendingSeenEvents(
                            userId,
                            conversationId
                    );

            log.info(
                    "[FIND_PENDING_SEEN_EVENTS_SUCCESS] userId={} conversationId={} count={}",
                    userId,
                    conversationId,
                    events.size()
            );

            return events;

        } catch (Exception ex) {
            log.error(
                    "[FIND_PENDING_SEEN_EVENTS_FAILED] userId={} conversationId={} error={}",
                    userId,
                    conversationId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }

    @Transactional
    public int bulkMarkSeen(
            Long userId,
            Long conversationId,
            LocalDateTime seenAt
    ) {
        log.info(
                "[BULK_MARK_SEEN] userId={} conversationId={}",
                userId,
                conversationId
        );

        try {
            int updatedRows =
                    messageReceiptRepository.bulkMarkSeen(
                            userId,
                            conversationId,
                            seenAt
                    );

            log.info(
                    "[BULK_MARK_SEEN_SUCCESS] userId={} conversationId={} updatedRows={}",
                    userId,
                    conversationId,
                    updatedRows
            );

            return updatedRows;

        } catch (Exception ex) {
            log.error(
                    "[BULK_MARK_SEEN_FAILED] userId={} conversationId={} error={}",
                    userId,
                    conversationId,
                    ex.getMessage(),
                    ex
            );

            throw ex;
        }
    }
}