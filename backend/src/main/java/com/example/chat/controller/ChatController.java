package com.example.chat.controller;

import com.example.chat.dto.*;
import com.example.chat.service.ChatOrchestrationService;
import com.example.chat.service.ChatServicePrivate;
import com.example.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatServicePrivate chatServicePrivate;
    private final ChatOrchestrationService chatOrchestrationService;
    private final UserService userService;

    // ── Send message ──────────────────────────────────────────────────────────

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long senderId = resolveUserId(userDetails);

        MessageDTO msg = chatServicePrivate.sendMessage(
                senderId,
                request.getReceiverId(),
                request.getContent(),
                request.getConversationId()
        );

        return ResponseEntity.ok(msg);
    }

    // ── Get conversations list ─────────────────────────────────────────────────

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationListDTO>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(chatOrchestrationService.getUserConversations(userId));
    }

    // ── Get messages ──────────────────────────────────────────────────────────

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long offsetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);

        List<MessageDTO> messages = offsetId == null
                ? chatServicePrivate.getMessages(conversationId, userId)
                : chatServicePrivate.getMessages(conversationId, userId, offsetId);

        return ResponseEntity.ok(messages);
    }

    // ── Mark seen ─────────────────────────────────────────────────────────────

    @PostMapping("/conversations/{conversationId}/seen")
    public ResponseEntity<Void> markSeen(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatOrchestrationService.markConversationSeen(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    // ── Mark all delivered ────────────────────────────────────────────────────

    @PostMapping("/markBulkDelivery")
    public ResponseEntity<Void> markAllDelivered(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatOrchestrationService.markAllDelivered(userId);
        return ResponseEntity.ok().build();
    }

    // ── Delete message for me ─────────────────────────────────────────────────

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}/me")
    public ResponseEntity<Void> deleteForMe(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatOrchestrationService.deleteMessageForMe(conversationId, messageId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Delete message for everyone ───────────────────────────────────────────

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}/everyone")
    public ResponseEntity<Void> deleteForEveryone(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatOrchestrationService.deleteMessageForEveryone(conversationId, messageId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Edit message ──────────────────────────────────────────────────────────

    @PatchMapping("/conversations/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> editMessage(
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatOrchestrationService.editMessage(conversationId, messageId, userId, request.getContent());
        return ResponseEntity.ok().build();
    }

    // ── Delete conversation for me ────────────────────────────────────────────

    @DeleteMapping("/conversations/{conversationId}/me")
    public ResponseEntity<Void> deleteConversationForMe(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatServicePrivate.deleteConversationForMe(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Delete conversation for everyone ──────────────────────────────────────

    @DeleteMapping("/conversations/{conversationId}/everyone")
    public ResponseEntity<Void> deleteConversationForEveryone(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);
        chatServicePrivate.deleteConversationForEveryone(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restore-eligible/{otherUserId}")
    public ResponseEntity<Boolean> canRestoreChat(
            @PathVariable Long otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);

        return ResponseEntity.ok(
                chatServicePrivate.userHasLastClosedChat(
                        userId,
                        otherUserId
                )
        );
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restoreChat(
            @RequestParam Long otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = resolveUserId(userDetails);

        chatServicePrivate.restoreLifecycle(
                userId,
                otherUserId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations/{conversationId}/history")
    public ResponseEntity<List<ConversationLifecycleDTO>> getConversationHistory(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        resolveUserId(userDetails); // optional auth check

        return ResponseEntity.ok(
                chatServicePrivate
                        .getPrivateConversationLifecycleHistory(conversationId)
        );
    }

    @GetMapping(
            "/conversations/{conversationId}/history/{conversationLifecycleId}/participants"
    )
    public ResponseEntity<List<ParticipantLifecycleDTO>>
    getParticipantLifecycles(
            @PathVariable Long conversationId,
            @PathVariable Long conversationLifecycleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Long userId = resolveUserId(userDetails);

        return ResponseEntity.ok(
                chatServicePrivate
                        .getPrivateParticipantLifecyclesOfConversationLifecycle(
                                conversationId,
                                conversationLifecycleId,
                                userId
                        )
        );
    }

    @GetMapping("/lifecycles/{participantLifecycleId}/messages")
    public ResponseEntity<List<MessageDTO>> loadLifecycleMessages(
            @PathVariable Long participantLifecycleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Long userId = resolveUserId(userDetails);

        return ResponseEntity.ok(
                chatServicePrivate
                        .loadMessagesOfLifecycle(
                                participantLifecycleId,
                                userId
                        )
        );
    }

    @GetMapping("/lifecycles/{participantLifecycleId}/messages/older")
    public ResponseEntity<List<MessageDTO>>
    loadOlderLifecycleMessages(
            @PathVariable Long participantLifecycleId,
            @RequestParam Long offsetId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Long userId = resolveUserId(userDetails);

        return ResponseEntity.ok    (
                chatServicePrivate
                        .loadMessagesOfLifecycle(
                                participantLifecycleId,
                                userId,
                                offsetId
                        )
        );
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private Long resolveUserId(UserDetails userDetails) {
        User user = userService.findByPhone(userDetails.getUsername());
        return user.getId();
    }
}