package com.example.chat.service;

import com.example.chat.conversation.entity.Conversation;
import com.example.chat.conversation.enums.ConversationType;
import com.example.chat.conversation.service.ConversationService;
import com.example.chat.conversationLifecycle.entity.ConversationLifecycle;
import com.example.chat.conversationLifecycle.service.ConversationLifecycleService;
import com.example.chat.conversationParticipant.service.ConversationParticipantService;
import com.example.chat.dto.ConversationLifecycleDTO;
import com.example.chat.dto.ConversationListDTO;
import com.example.chat.dto.MessageDTO;
import com.example.chat.dto.ParticipantLifecycleDTO;
import com.example.chat.group.enums.GroupType;
import com.example.chat.message.entity.ChatMessage;
import com.example.chat.message.service.MessageService;
import com.example.chat.messageReceipt.entity.MessageReceipt;
import com.example.chat.messageReceipt.service.MessageReceiptService;
import com.example.chat.participantLifecycle.entity.ParticipantLifecycle;
import com.example.chat.participantLifecycle.service.ParticipantLifecycleService;
import com.example.eventLog.enums.AggregateType;
import com.example.eventLog.enums.EventType;
import com.example.eventLog.service.EventLogService;
import com.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServicePrivate {

    private final ConversationService conversationService;
    private final ConversationParticipantService participantService;
    private final MessageService messageService;
    private final MessageReceiptService receiptService;
    private final ConversationLifecycleService conversationLifecycleService;
    private final ParticipantLifecycleService participantLifecycleService;
    private final UserService userService;
    private final ChatQueryHelper chatQueryHelper;
    private final EventLogService eventLogService;
    private final SimpMessagingTemplate messagingTemplate;

    // ===== SEND PRIVATE MESSAGE (handles both first message and subsequent) =====
    @Transactional
    public MessageDTO sendMessage(Long senderId, Long receiverId, String content, Long conversationId) {

        log.info("[SEND_PRIVATE_MESSAGE] sender={} receiver={} conversationId={}",
                senderId, receiverId, conversationId);

        // 1 — Validate receiver exists
        userService.validateExists(receiverId);

        String senderHandleName=userService.getHandleNameById(senderId);

        String receiverHandleName=userService.getHandleNameById(receiverId);

        Conversation conversation;

        if (conversationId == null) {

            // NEW CHAT — find or create conversation
            String pairKey = Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);
            log.debug("[PAIR_KEY] {}", pairKey);

            conversation = conversationService
                    .findByTypeAndPairKey(ConversationType.PRIVATE, pairKey)
                    .orElse(null);

            if (conversation == null) {
                log.info("[NO_CONVO] Creating new conversation pairKey={}", pairKey);

                conversation = conversationService.createPrivateConversation(senderId, receiverId);

                eventLogService.logEvent(
                        AggregateType.CONVERSATION,
                        conversation.getId(),
                        EventType.CONVERSATION_CREATED,
                        senderId,
                        Map.of(
                                "conversationId", conversation.getId(),
                                "type", ConversationType.PRIVATE.name(),
                                "creatorId", senderId,
                                "pairKey", conversation.getPairKey()
                        )
                );


                participantService.addParticipantsInPrivate(conversation, senderId
                        , receiverId);

                boolean check=conversationLifecycleService.startIfNotExists(conversation);
                if(check){
                    eventLogService.logEvent(
                            AggregateType.CONVERSATION,
                            conversation.getId(),
                            EventType.CONVERSATION_OPENED,
                            senderId,
                            Map.of(
                                    "conversationId", conversation.getId()
                            )
                    );
                }
                check=participantLifecycleService.startIfNotExists(conversation.getId(), senderId);
                if(check){
                    eventLogService.logEvent(
                            AggregateType.CONVERSATION,
                            conversation.getId(),
                            EventType.PARTICIPANT_JOINED,
                            senderId,
                            Map.of(
                                    "userId", senderId
                            )
                    );
                }
                check=participantLifecycleService.startIfNotExists(conversation.getId(), receiverId);
                if(check){
                    eventLogService.logEvent(
                            AggregateType.CONVERSATION,
                            conversation.getId(),
                            EventType.PARTICIPANT_JOINED,
                            senderId, // actor = creator who added participant
                            Map.of(
                                    "userId", receiverId
                            )
                    );
                }

            } else {
                log.info("[CONVO_EXISTS] id={} ensuring lifecycles", conversation.getId());

                // Conversation row exists but lifecycle may be closed — restart if needed
                boolean check=conversationLifecycleService.startIfNotExists(conversation);
                if(check){
                    eventLogService.logEvent(
                            AggregateType.CONVERSATION,
                            conversation.getId(),
                            EventType.CONVERSATION_OPENED,
                            senderId,
                            Map.of(
                                    "conversationId", conversation.getId()
                            )
                    );
                }
                check=participantLifecycleService.startIfNotExists(conversation.getId(), senderId);
                if(check){
                    eventLogService.logEvent(
                            AggregateType.CONVERSATION,
                            conversation.getId(),
                            EventType.PARTICIPANT_JOINED,
                            senderId,
                            Map.of(
                                    "userId", senderId
                            )
                    );
                }
                check=participantLifecycleService.startIfNotExists(conversation.getId(), receiverId);
                if(check){
                    eventLogService.logEvent(
                            AggregateType.CONVERSATION,
                            conversation.getId(),
                            EventType.PARTICIPANT_JOINED,
                            senderId, // actor = creator who added participant
                            Map.of(
                                    "userId", receiverId
                            )
                    );
                }
            }

        } else {

            // EXISTING CHAT — validate and send
            conversation = conversationService
                    .getById(conversationId);

            // 2 — Sender must be an active participant
            participantLifecycleService.validateActiveParticipant(conversationId, senderId);

            // 3 — Receiver lifecycle: reopen if they had deleted for me (independent lifecycles)
            boolean check=participantLifecycleService.startIfNotExists(conversationId, receiverId);
            if(check){
                eventLogService.logEvent(
                        AggregateType.CONVERSATION,
                        conversation.getId(),
                        EventType.PARTICIPANT_JOINED,
                        senderId, // actor = creator who added participant
                        Map.of(
                                "userId", receiverId
                        )
                );
            }
        }

        // 4 — Save message
        ChatMessage saved = messageService.savePrivateMessage(conversation, senderId, content);

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversation.getId(),
                EventType.MESSAGE_SENT,
                senderId,
                Map.of(
                        "messageId", saved.getId(),
                        "conversationId", conversation.getId(),
                        "senderId", senderId,
                        "receiverId",receiverId,
                        "content", saved.getContent(),
                        "createdAt", saved.getCreatedAt()
                )
        );


        // 5 — Eager receipt creation for both participants
        receiptService.createInitialReceipts(saved, senderId,senderHandleName, receiverId);

        // 6 — Update conversation ordering timestamp
        conversationService.updateLastTime(conversation, saved.getCreatedAt());

        MessageReceipt receiverReceipt=receiptService.getReceiptByMessageAndUser(saved.getId(),receiverId);

        log.info("[SEND_PRIVATE_MESSAGE_SUCCESS] messageId={} convoId={}",
                saved.getId(), conversation.getId());

        return chatQueryHelper.toMessageDTO(saved,conversation.getId(), receiverReceipt.isDelivered(),
                receiverReceipt.isSeen(),senderHandleName);
    }

    // ===== DELETE CHAT FOR ME =====
    @Transactional
    public void deleteConversationForMe(Long conversationId, Long userId) {

        log.info("[DELETE_CHAT_FOR_ME] convoId={} userId={}", conversationId, userId);

        conversationService.getById(conversationId);

        // Authorization — only active participant can delete for themselves
        participantLifecycleService.validateActiveParticipant(conversationId, userId);
        participantLifecycleService.endParticipantLifecycle(conversationId, userId,LocalDateTime.now());

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversationId,
                EventType.PARTICIPANT_LEFT,
                userId,
                Map.of(
                        "userId", userId
                )
        );

        log.info("[CHAT_HIDDEN_FOR_USER] convoId={} userId={}", conversationId, userId);
    }

    // ===== DELETE CHAT FOR EVERYONE =====
    @Transactional
    public void deleteConversationForEveryone(Long conversationId, Long userId) {

        log.info("[DELETE_PRIVATE_CHAT_FOR_EVERYONE] convoId={} userId={}", conversationId, userId);

        Conversation conversation=conversationService.getById(conversationId);

        // Authorization — only active participant can delete for everyone
        participantLifecycleService.validateActiveParticipant(conversationId, userId);

        LocalDateTime now=LocalDateTime.now();

        // End conversation lifecycle globally
        conversationLifecycleService.endConversationLifecycle(conversationId,now);



        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversationId,
                EventType.CONVERSATION_CLOSED,
                userId,
                Map.of(
                        "conversationId", conversationId,
                        "endedBy", userId,
                        "endedAt", LocalDateTime.now()
                )
        );

        // End both participant lifecycles
        participantLifecycleService.endParticipantLifecycle(conversationId, userId,now);

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversation.getId(),
                EventType.PARTICIPANT_LEFT,
                userId,
                Map.of(
                        "userId", userId
                )
        );

        Long otherUserId = participantService.findOtherParticipant(conversationId, userId);
        participantLifecycleService.endParticipantLifecycle(conversationId, otherUserId,now);

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversation.getId(),
                EventType.PARTICIPANT_LEFT,
                userId,
                Map.of(
                        "userId", otherUserId
                )
        );

        messagingTemplate.convertAndSendToUser(
                otherUserId.toString(),
                "/queue/messages",
                Map.of(
                        "type", "CONVERSATION_DELETED",
                        "conversationId", conversationId
                )
        );

        log.info("[DELETE_PRIVATE_CHAT_FOR_EVERYONE_COMPLETED] convoId={}", conversationId);
    }

    // ===== GET MESSAGES (latest window, no offset) =====
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long conversationId, Long userId) {

        log.info("[GET_MESSAGES] convoId={} userId={}", conversationId, userId);

        conversationService.getById(conversationId);
        conversationLifecycleService.validateActive(conversationId);
        participantService.validateParticipant(conversationId,userId);

        ParticipantLifecycle lifecycle = participantLifecycleService
                .getActiveLifecycle(conversationId, userId)
                .orElse(null);

        if (lifecycle == null) {
            log.warn("[NO_ACTIVE_PARTICIPATION] convoId={} userId={}", conversationId, userId);
            return List.of();
        }

        LocalDateTime visibleFrom = lifecycle.getJoinedAt();

        return chatQueryHelper.fetchMessages(conversationId, userId, null,visibleFrom);
    }

    // ===== GET MESSAGES (paginated with offsetId) =====
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long conversationId, Long userId, Long offsetId) {

        log.info("[GET_OLDER_MESSAGES] convoId={} userId={} offsetId={}", conversationId, userId, offsetId);

        conversationService.getById(conversationId);
        conversationLifecycleService.validateActive(conversationId);

        ParticipantLifecycle lifecycle = participantLifecycleService
                .getActiveLifecycle(conversationId, userId)
                .orElse(null);

        if (lifecycle == null) {
            log.warn("[NO_ACTIVE_PARTICIPATION] convoId={} userId={}", conversationId, userId);
            return List.of();
        }

        LocalDateTime visibleFrom = lifecycle.getJoinedAt();

        return chatQueryHelper.fetchMessages(conversationId, userId, offsetId,visibleFrom);
    }

    // ===== CONVERSATION LIFECYCLE HISTORY (time-travel) =====
    @Transactional(readOnly = true)
    public List<ConversationLifecycleDTO> getPrivateConversationLifecycleHistory(Long conversationId) {

        log.info("[LIFECYCLE_HISTORY] conversationId={}", conversationId);

        List<ConversationLifecycleDTO> lifecycleDTOS=
                conversationLifecycleService.getPrivateConversationLifecycleHistory(conversationId);


        return lifecycleDTOS;
    }

    // ===== PARTICIPANT LIFECYCLES OF A CONVERSATION LIFECYCLE (time-travel) =====
    @Transactional(readOnly = true)
    public List<ParticipantLifecycleDTO> getPrivateParticipantLifecyclesOfConversationLifecycle(
            Long conversationId,
            Long conversationLifecycleId,
            Long requestingUserId
    ) {
        log.info("[PARTICIPANT_LIFECYCLES] convoId={} lifecycleId={} requestingUser={}",
                conversationId, conversationLifecycleId, requestingUserId);

        // Authorization — requesting user must be a participant of this conversation
        participantService.validateParticipant(conversationId, requestingUserId);

        // Service already validates conversationLifecycleId belongs to conversationId
        ConversationLifecycle conversationLifecycle = conversationLifecycleService
                .getConversationLifeCycle(conversationId, conversationLifecycleId);

        return participantLifecycleService
                .getPrivateParticipantLifecyclesOfConversationLifecycle(
                        conversationId,
                        conversationLifecycle.getStartedAt(),
                        conversationLifecycle.getEndedAt(),
                        requestingUserId
                );
    }

    // ===== LOAD MESSAGES OF A SPECIFIC PARTICIPANT LIFECYCLE (time-travel, latest) =====
    @Transactional(readOnly = true)
    public List<MessageDTO> loadMessagesOfLifecycle(Long participantLifecycleId, Long userId) {

        log.info("[LOAD_LIFECYCLE_MESSAGES] plId={} userId={}", participantLifecycleId, userId);

        // getLifecycleById validates ownership (userId must match lifecycle's userId)
        ParticipantLifecycle pl = participantLifecycleService
                .getLifecycleById(participantLifecycleId, userId);

        return chatQueryHelper.fetchLifecycleMessages(pl, userId, null);
    }

    // ===== LOAD MESSAGES OF A SPECIFIC PARTICIPANT LIFECYCLE (time-travel, paginated) =====
    @Transactional(readOnly = true)
    public List<MessageDTO> loadMessagesOfLifecycle(
            Long participantLifecycleId,
            Long userId,
            Long offsetId
    ) {
        log.info("[LOAD_OLDER_LIFECYCLE_MESSAGES] plId={} userId={} offsetId={}",
                participantLifecycleId, userId, offsetId);

        ParticipantLifecycle pl = participantLifecycleService
                .getLifecycleById(participantLifecycleId, userId);

        return chatQueryHelper.fetchLifecycleMessages(pl, userId, offsetId);
    }

    @Transactional
    public void restoreLifecycle(Long senderId, Long receiverId) {

        log.info("[RESTORE_LIFECYCLE] senderId={} receiverId={}", senderId, receiverId);

        // 1 — Derive pairKey
        String pairKey = Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);

        // 2 — Find conversation — nothing to restore if it never existed
        Conversation conversation = conversationService
                .findByTypeAndPairKey(ConversationType.PRIVATE, pairKey)
                .orElseThrow(() -> new IllegalStateException("No conversation found to restore"));

        Long conversationId = conversation.getId();

        // 3 — Reject if sender already has an active lifecycle
        boolean alreadyActive = participantLifecycleService
                .getActiveLifecycle(conversationId, senderId)
                .isPresent();

        if (alreadyActive) {
            log.warn("[RESTORE_REJECTED] Active lifecycle already exists. convoId={} userId={}",
                    conversationId, senderId);
            throw new IllegalStateException("Chat is already active — nothing to restore");
        }

        // 4 — Find last closed lifecycle for sender
        ParticipantLifecycle lastClosed = participantLifecycleService
                .findLastClosedLifecycle(conversationId, senderId)
                .orElseThrow(() -> new IllegalStateException("No closed lifecycle found to restore"));

        // 5 — If conversation lifecycle ended, restore it
        boolean check=conversationLifecycleService.undoLifecycle(conversation.getId());
        if(check){
            eventLogService.logEvent(
                    AggregateType.CONVERSATION,
                    conversation.getId(),
                    EventType.LAST_CONVERSATION_REOPENED,
                    senderId,
                    Map.of(
                            "conversationId", conversation.getId()
                    )
            );
        }

        // 6 — Undo the delete: set leftAt = null on the last closed lifecycle
        participantLifecycleService.undoLifecycleClose(lastClosed);

        eventLogService.logEvent(
                AggregateType.CONVERSATION,
                conversationId,
                EventType.PARTICIPANT_RESTORED,
                senderId,
                Map.of(
                        "userId", senderId,
                        "restoredLifecycleId", lastClosed.getId()
                )
        );

        log.info("[RESTORE_LIFECYCLE_SUCCESS] convoId={} senderId={}", conversationId, senderId);
    }

    @Transactional(readOnly = true)
    public boolean userHasLastClosedChat(Long senderId, Long receiverId) {

        log.info("[CHECK_RESTORE_ELIGIBILITY] userId={}, otherUserId={}", senderId, receiverId);

        try {
            // 1 — Build pair key (same logic you already use everywhere)
            String pairKey = Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);

            // 2 — Find conversation by pair key
            Optional<Conversation> optionalConversation =
                    conversationService.findByTypeAndPairKey(ConversationType.PRIVATE, pairKey);

            if (optionalConversation.isEmpty()) {
                log.info("[CHECK_RESTORE_ELIGIBILITY] No conversation found for pairKey={}", pairKey);
                return false;
            }

            Long conversationId = optionalConversation.get().getId();

            // 3 — Check if user has a last closed lifecycle
            boolean hasClosedLifecycle =
                    participantLifecycleService
                            .findLastClosedLifecycle(conversationId, senderId)
                            .isPresent();

            log.info("[CHECK_RESTORE_ELIGIBILITY] result={}, conversationId={}",
                    hasClosedLifecycle, conversationId);

            return hasClosedLifecycle;

        } catch (Exception ex) {
            log.error("[CHECK_RESTORE_ELIGIBILITY_ERROR] userId={}, otherUserId={}",
                    senderId, receiverId, ex);
            throw ex; // let global handler deal with it
        }
    }






}