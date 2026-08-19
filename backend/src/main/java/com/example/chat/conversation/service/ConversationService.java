package com.example.chat.conversation.service;

import com.example.chat.conversation.entity.Conversation;
import com.example.chat.conversation.enums.ConversationType;
import com.example.chat.conversation.repo.ConversationRepository;
import com.example.chat.dto.ConversationListDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;



    @Transactional
    public Conversation createPrivateConversation(Long senderId, Long receiverId) {

        String pairKey = Math.min(senderId, receiverId) + "_" + Math.max(senderId, receiverId);
        log.debug("[CREATE_PRIVATE_CONVO] pairKey={}", pairKey);

        Conversation conversation = Conversation.builder()
                .type(ConversationType.PRIVATE)
                .pairKey(pairKey)
                .build();

        try {
            Conversation saved = conversationRepository.save(conversation);
            log.info("[CONVO_CREATED] id={}, pairKey={}", saved.getId(), pairKey);
            return saved;

        } catch (Exception ex) {
            log.error("[CONVO_CREATE_FAILED] pairKey={}, error={}", pairKey, ex.getMessage(), ex);
            return conversationRepository
                    .findByTypeAndPairKey(ConversationType.PRIVATE, pairKey)
                    .orElseThrow(() -> new IllegalStateException("Conversation exists but cannot fetch"));
        }
    }


    public Optional<Conversation> findByTypeAndPairKey(ConversationType conversationType,String pairKey){
        return conversationRepository.findByTypeAndPairKey(conversationType,pairKey);
    }

    public void updateLastTime(Conversation conversation,LocalDateTime localDateTime) {
        log.info("[UPDATING_CONVO_LAST_MESSAGE_TIME] id={} type={}", conversation.getId(), conversation.getType());
        try{
            conversation.setLastMessageAt(localDateTime);
            conversationRepository.save(conversation);
            log.info("[UPDATE_SUCCESS] id={} type={}", conversation.getId(), conversation.getType());
        }catch(Exception ex){
            log.error("[UPDATE_FAILED] id={} type={}", conversation.getId(), conversation.getType(),ex);
        }
    }


    @Transactional(readOnly = true)
    public List<Conversation> findAllByIdsOrderByLastMessage(List<Long> conversationIds) {

        log.info("[FETCH_CONVERSATIONS_BY_IDS] idsCount={}", conversationIds.size());

        try {

            List<Conversation> conversations =
                    conversationRepository.findAllByIdsOrderByLastMessage(conversationIds);

            log.info("[FETCH_CONVERSATIONS_BY_IDS_SUCCESS] count={}", conversations.size());

            return conversations;

        } catch (Exception ex) {
            log.error("[FETCH_CONVERSATIONS_BY_IDS_FAILED] idsCount={}", conversationIds.size(), ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ConversationListDTO> findConversationListForUser(
            List<Long> conversationIds,
            Long userId) {

        log.info("[FETCH_CHAT_LIST_JOINED] userId={} conversations={}",
                userId, conversationIds.size());

        try {
            List<ConversationListDTO> result =
                    conversationRepository.findConversationListForUser(conversationIds, userId);

            log.info("[CHAT_LIST_SUCCESS] userId={} count={}",
                    userId, result.size());

            return result;

        } catch (Exception ex) {
            log.error("[CHAT_LIST_FAILED] userId={}", userId, ex);
            throw ex;
        }
    }

    public Conversation getById(Long conversationId) {
        log.info("[FINDING_CONVO] id={}", conversationId);

        return conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> {
                    log.warn("[CONVO_NOT_FOUND] id={}",conversationId);
                    return new IllegalStateException("Invalid conversationId");
                });
    }

    @Transactional
    public Conversation createGroupConversation() {

        log.info("[GROUP_CONVERSATION_CREATE_START]");

        try {
            Conversation conversation = Conversation.builder()
                    .type(ConversationType.GROUP)
                    .pairKey(null)
                    .build();

            Conversation saved = conversationRepository.save(conversation);

            log.info("[GROUP_CONVERSATION_CREATE_SUCCESS] conversationId={}",
                    saved.getId());

            return saved;

        } catch (Exception ex) {

            log.error("[GROUP_CONVERSATION_CREATE_FAILED] reason={}",
                    ex.getMessage(), ex);

            throw new RuntimeException("Failed to create group conversation", ex);
        }
    }
}