package com.example.chat.service;

import com.example.chat.conversation.service.ConversationService;
import com.example.chat.dto.ConversationListDTO;
import com.example.chat.dto.DeliveryEventDTO;
import com.example.chat.message.entity.ChatMessage;
import com.example.chat.message.service.MessageService;
import com.example.chat.messageReceipt.entity.MessageReceipt;
import com.example.chat.messageReceipt.service.MessageReceiptService;
import com.example.chat.participantLifecycle.entity.ParticipantLifecycle;
import com.example.chat.participantLifecycle.service.ParticipantLifecycleService;
import com.example.eventLog.enums.AggregateType;
import com.example.eventLog.enums.EventType;
import com.example.eventLog.service.EventLogService;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import com.example.websocket.dto.MessageDeletedEvent;
import com.example.websocket.service.PresenceService;
import com.example.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatOrchestrationService {

    private final ParticipantLifecycleService participantLifecycleService;
    private final MessageReceiptService receiptService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final EventLogService eventLogService;
    private final PresenceService presenceService;
    private final UserService userService;
    private final WebSocketService websocketService;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public void markAllDelivered(Long userId) {

        log.info("[MARK_ALL_DELIVERED] userId={}", userId);

        List<DeliveryEventDTO> events =
                receiptService.findPendingDeliveryEvents(userId);

        if (events.isEmpty()) {
            log.info("[MARK_ALL_DELIVERED_SKIP] No pending receipts. userId={}", userId);
            return;
        }

        int updatedRows =
                receiptService.bulkMarkDelivered(userId, LocalDateTime.now());

        log.info(
                "[MARK_ALL_DELIVERED_UPDATED] userId={} rows={}",
                userId,
                updatedRows
        );

        events.forEach(event -> {
            messagingTemplate.convertAndSendToUser(
                    event.getSenderId().toString(),
                    "/queue/messages",
                    Map.of(
                            "type", "MESSAGE_DELIVERED",
                            "messageId", event.getMessageId(),
                            "conversationId", event.getConversationId()
                    )
            );
        });

        log.info("[MARK_ALL_DELIVERED_SUCCESS] userId={}", userId);
    }

    @Transactional
    public void markConversationSeen(
            Long userId,
            Long conversationId
    ) {
        log.info(
                "[MARK_CONVERSATION_SEEN] userId={} conversationId={}",
                userId,
                conversationId
        );

        List<DeliveryEventDTO> events =
                receiptService.findPendingSeenEvents(
                        userId,
                        conversationId
                );

        if (events.isEmpty()) {
            log.info(
                    "[MARK_CONVERSATION_SEEN_SKIP] userId={} conversationId={}",
                    userId,
                    conversationId
            );
            return;
        }

        receiptService.bulkMarkSeen(
                userId,
                conversationId,
                LocalDateTime.now()
        );

        events.forEach(event ->
                messagingTemplate.convertAndSendToUser(
                        event.getSenderId().toString(),
                        "/queue/messages",
                        Map.of(
                                "type", "MESSAGE_SEEN",
                                "messageId", event.getMessageId(),
                                "conversationId", event.getConversationId()
                        )
                )
        );

        log.info(
                "[MARK_CONVERSATION_SEEN_SUCCESS] userId={} conversationId={} events={}",
                userId,
                conversationId,
                events.size()
        );
    }

    // ===== GET USER CONVERSATION LIST =====
    @Transactional(readOnly = true)
    public List<ConversationListDTO> getUserConversations(Long userId) {

        log.info("[CHAT_LIST_FETCH] userId={}", userId);

        List<ParticipantLifecycle> lifecycles =
                participantLifecycleService.findActiveByUser(userId);

        if (lifecycles.isEmpty()) {
            log.info("[NO_ACTIVE_CONVERSATIONS] userId={}", userId);
            return List.of();
        }

        List<Long> conversationIds = lifecycles.stream()
                .map(ParticipantLifecycle::getConversationId)
                .toList();

        // Single joined query — returns conversationId, otherUserId, username, lastMessageAt
        // Eliminates N+1: no per-row calls to participantService or userService
        List<ConversationListDTO> result = conversationService
                .findConversationListForUser(conversationIds, userId);

        result.forEach(dto -> {
            Long otherUserId= dto.getOtherUserId();
            dto.setOnline(
                    presenceService.isOnline(otherUserId)
            );

            User otherUser=userService.findById(otherUserId);
            dto.setLastSeen(
                    otherUser.getLastSeen()
            );

            // Last message
            messageService.findLastMessage(dto.getConversationId())
                    .ifPresent(msg -> {
                        dto.setLastMessageId(msg.getId());
                        dto.setLastMessage(msg.isDeletedForEveryone()
                                ? "Message deleted"
                                : msg.getContent());
                        dto.setLastMessageDeleted(msg.isDeletedForEveryone());
                    });

            // Unread count — within active lifecycle window
            ParticipantLifecycle lc = lifecycles.stream()
                    .filter(l -> l.getConversationId().equals(dto.getConversationId()))
                    .findFirst().orElse(null);

            if (lc != null) {
                long unread = receiptService.countUnread(
                        dto.getConversationId(), userId, lc.getJoinedAt());
                dto.setUnreadCount(unread);
            }
        });

        log.info("[CHAT_LIST_SUCCESS] userId={} count={}", userId, result.size());

        return result;
    }

    @Transactional
    public void deleteMessageForEveryone(Long conversationId, Long messageId, Long userId) {

        log.info("[DELETE_MESSAGE_FOR_EVERYONE] convoId={} messageId={} userId={}",
                conversationId, messageId, userId);

        // 1 — Validate conversation
        conversationService.getById(conversationId);

        // 2 — Validate sender has active lifecycle
        participantLifecycleService.validateActiveParticipant(conversationId, userId);

        // 3 — Fetch message and validate sender ownership
        ChatMessage message = messageService.getById(messageId);

        if(!message.getSenderId().equals(userId)){
            log.warn("[DELETE_FOR_EVERYONE_REJECTED] Message can only be deleted by sender. messageId={} userId={}",
                    messageId, userId);
            throw new IllegalStateException("Cannot delete for everyone a message you have not sent");
        }

        // 4 — Sender's own receipt check — if deleted for me, they have no visibility over it
        MessageReceipt senderReceipt = receiptService.getReceiptByMessageAndUser(messageId, userId);

        if (senderReceipt.isDeletedForMe()) {
            log.warn("[DELETE_FOR_EVERYONE_REJECTED] Message already deleted for sender. messageId={} userId={}",
                    messageId, userId);
            throw new IllegalStateException("Cannot delete for everyone a message you have already deleted for yourself");
        }

        // 5 — Already deleted for everyone
        if (message.isDeletedForEveryone()) {
            log.warn("[DELETE_FOR_EVERYONE_REJECTED] Already deleted for everyone. messageId={}", messageId);
            throw new IllegalStateException("Message is already deleted for everyone");
        }

        ChatMessage latest =
                messageService
                        .findFirstByConversationIdAndDeletedForEveryoneFalseOrderByCreatedAtDesc(
                                conversationId
                        )
                        .orElse(null);

        boolean isLastMessage =
                latest != null &&
                        latest.getId().equals(messageId);

        // 6 — Set deletedForEveryone = true
        messageService.markDeletedForEveryone(message);



        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversationId,
                EventType.MESSAGE_DELETED_FOR_EVERYONE,
                userId,
                Map.of(
                        "messageId", messageId
                )
        );

        MessageDeletedEvent event =
                new MessageDeletedEvent(
                        "MESSAGE_DELETED",
                        conversationId,
                        messageId,
                        true
                );

        List<MessageReceipt> receipts =
                receiptService.findByMessageId(messageId);

        Long otherUserId=0L;

        for (MessageReceipt receipt : receipts) {
            if(!userId.equals(receipt.getId())){
                otherUserId=receipt.getUserId();
            }

            websocketService.sendToUser(
                    receipt.getUserId(),
                    event
            );

        }

        messagingTemplate.convertAndSendToUser(
                otherUserId.toString(),
                "/queue/messages",
                Map.of(
                        "type", "MESSAGE_DELETED",
                        "messageId", messageId,
                        "conversationId", conversationId,
                        "isLastMessage", isLastMessage


                )
        );


        log.info("[DELETE_MESSAGE_FOR_EVERYONE_SUCCESS] messageId={}", messageId);
    }

    @Transactional
    public void deleteMessageForMe(Long conversationId, Long messageId, Long userId) {

        log.info("[DELETE_MESSAGE_FOR_ME] convoId={} messageId={} userId={}",
                conversationId, messageId, userId);

        // 1 — Validate conversation
        conversationService.getById(conversationId);

        // 2 — Validate user has active lifecycle
        participantLifecycleService.validateActiveParticipant(conversationId, userId);

        // 3 — Fetch user's receipt
        MessageReceipt receipt = receiptService.getReceiptByMessageAndUser(messageId, userId);

        // 4 — Already deleted for me
        if (receipt.isDeletedForMe()) {
            log.warn("[DELETE_FOR_ME_REJECTED] Already deleted for me. messageId={} userId={}",
                    messageId, userId);
            throw new IllegalStateException("Message already deleted for me");
        }

        // 5 — Set deletedForMe = true on receipt only — ChatMessage untouched
        receiptService.markDeletedForMe(receipt);

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversationId,
                EventType.MESSAGE_DELETED_FOR_ME,
                userId,
                Map.of(
                        "messageId", messageId,
                        "userId", userId
                )
        );

        log.info("[DELETE_MESSAGE_FOR_ME_SUCCESS] messageId={} userId={}", messageId, userId);
    }

    @Transactional
    public void editMessage(Long conversationId, Long messageId, Long userId, String newContent) {

        log.info("[EDIT_MESSAGE] convoId={} messageId={} userId={}", conversationId, messageId, userId);

        // 1 — Validate conversation
        conversationService.getById(conversationId);

        // 2 — Validate sender has active lifecycle
        participantLifecycleService.validateActiveParticipant(conversationId, userId);

        // 3 — Fetch message and validate ownership
        ChatMessage message = messageService.getByIdAndSender(messageId, userId);

        // 4 — Cannot edit a deleted message
        if (message.isDeletedForEveryone()) {
            log.warn("[EDIT_REJECTED] Message deleted for everyone. messageId={}", messageId);
            throw new IllegalStateException("Cannot edit a message deleted for everyone");
        }

        String oldContent= message.getContent();

        // 5 — Update content and editedAt only — receipts are never touched
        messageService.editMessage(message, newContent);

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversationId,
                EventType.MESSAGE_EDITED,
                userId,
                Map.of(
                        "messageId", messageId,
                        "oldContent", oldContent,
                        "newContent", newContent
                )
        );

        log.info("[EDIT_MESSAGE_SUCCESS] messageId={}", messageId);
    }

}
