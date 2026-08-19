package com.example.chat.service;


import com.example.chat.conversation.entity.Conversation;
import com.example.chat.conversation.service.ConversationService;
import com.example.chat.conversationLifecycle.service.ConversationLifecycleService;
import com.example.chat.conversationParticipant.service.ConversationParticipantService;
import com.example.chat.dto.GroupProfileDTO;
import com.example.chat.group.entities.GroupProfile;
import com.example.chat.group.enums.GroupType;
import com.example.chat.group.service.GroupAdminService;
import com.example.chat.group.service.GroupInviteService;
import com.example.chat.group.service.GroupProfileService;
import com.example.chat.message.service.MessageService;
import com.example.chat.messageReceipt.service.MessageReceiptService;
import com.example.chat.participantLifecycle.entity.ParticipantLifecycle;
import com.example.chat.participantLifecycle.service.ParticipantLifecycleService;
import com.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceGroup {

    private final ConversationService conversationService;
    private final ConversationParticipantService participantService;
    private final MessageService messageService;
    private final MessageReceiptService receiptService;
    private final ConversationLifecycleService conversationLifecycleService;
    private final ParticipantLifecycleService participantLifecycleService;
    private final UserService userService;
    private final ChatQueryHelper chatQueryHelper;
    private final GroupProfileService groupProfileService;
    private final GroupAdminService groupAdminService;
    private final GroupInviteService groupInviteService;

    @Transactional
    public GroupProfileDTO createGroup(
            Long creatorId,
            String name,
            String description,
            String avatar,
            String groupType,
            List<Long> initialMemberIds
    ) {

        log.info("[CREATE_GROUP] creatorId={} groupType={} initialMembers={}",
                creatorId, groupType, initialMemberIds.size());

        // 1 — Validate creator exists
        userService.validateExists(creatorId);

        // 2 — Validate initial members exist if any provided
        if (initialMemberIds != null && !initialMemberIds.isEmpty()) {
            userService.validateAllExist(initialMemberIds);
        }

        // 3 — Create Conversation
        Conversation conversation = conversationService
                .createGroupConversation();

        Long groupId = conversation.getId();
        log.debug("[GROUP_CONVO_CREATED] groupId={}", groupId);

        // 4 — Create GroupProfile
        groupProfileService.createGroupProfile(
                conversation,
                name,
                description,
                avatar,
                GroupType.valueOf(groupType),
                creatorId
        );

        // 5 — Open ConversationLifecycle
        conversationLifecycleService.startIfNotExists(conversation);

        // 6 — Creator setup
        participantService.addParticipant(conversation, creatorId);
        participantLifecycleService.startIfNotExists(groupId, creatorId);
        groupAdminService.addCreatorAsAdmin(groupId, creatorId);

        log.info("[CREATOR_SETUP_COMPLETE] groupId={} creatorId={}", groupId, creatorId);

        // 7 — Send invites to initial members if any
        if (initialMemberIds != null && !initialMemberIds.isEmpty()) {
            for (Long memberId : initialMemberIds) {
                groupInviteService.createDirectInvite(groupId, memberId, creatorId);
            }
            log.info("[INITIAL_INVITES_SENT] groupId={} count={}", groupId, initialMemberIds.size());
        }

        log.info("[CREATE_GROUP_SUCCESS] groupId={} creatorId={}", groupId, creatorId);

        return groupProfileService.getGroupProfileDTO(groupId);
    }

    @Transactional
    public void updateGroupProfile(
            Long groupId,
            Long adminId,
            String name,
            String description,
            String avatar
    ) {

        log.info("[UPDATE_GROUP_PROFILE] groupId={} adminId={}", groupId, adminId);

        // 1 — Validate conversation exists and is GROUP type
        conversationService.getById(groupId);

        // 2 — Validate group is operational
        conversationLifecycleService.validateActive(groupId);

        // 3 — Validate adminId is an admin
        groupAdminService.validateAdmin(groupId, adminId);

        // 4 — Validate name is not null or blank
        if (name == null || name.isBlank()) {
            log.warn("[UPDATE_GROUP_PROFILE_REJECTED] Name is null or blank. groupId={}", groupId);
            throw new IllegalArgumentException("Group name must not be null or blank");
        }

        // 5 — Fetch GroupProfile
        GroupProfile profile = groupProfileService.getByGroupId(groupId);

        // 6 — Update fields
        // name — always updated, validated above
        // description — null clears it
        // avatar — null clears it
        groupProfileService.updateProfile(profile, name, description, avatar);

        log.info("[UPDATE_GROUP_PROFILE_SUCCESS] groupId={} adminId={}", groupId, adminId);
    }

    @Transactional
    public void modifyGroupType(Long groupId, Long adminId) {

        log.info("[MODIFY_GROUP_TYPE] groupId={} adminId={}", groupId, adminId);

        // 1 — Validate conversation exists
        conversationService.getById(groupId);

        // 2 — Validate group is operational
        conversationLifecycleService.validateActive(groupId);

        // 3 — Validate adminId is an admin
        groupAdminService.validateAdmin(groupId, adminId);

        // 4 — Fetch GroupProfile
        GroupProfile profile = groupProfileService.getByGroupId(groupId);

        // 5 — Validate current type is BOUNDED
        if (profile.getGroupType() != GroupType.BOUNDED) {
            log.warn("[MODIFY_GROUP_TYPE_REJECTED] Group is already OPEN. groupId={}", groupId);
            throw new IllegalStateException("Group is already OPEN — no modification needed");
        }

        // 6 — Update groupType to OPEN
        groupProfileService.updateGroupType(profile, GroupType.OPEN);

        log.info("[MODIFY_GROUP_TYPE_SUCCESS] groupId={} adminId={}", groupId, adminId);
    }

    @Transactional
    public void removeMember(Long groupId, Long adminId, Long targetUserId) {

        log.info("[REMOVE_MEMBER] groupId={} adminId={} targetUserId={}",
                groupId, adminId, targetUserId);

        // 1 — Validate conversation exists
        conversationService.getById(groupId);

        // 2 — Validate group is operational
        conversationLifecycleService.validateActive(groupId);

        // 3 — Validate adminId is an admin
        groupAdminService.validateAdmin(groupId, adminId);

        // 4 — Cannot remove yourself via this method
        if (adminId.equals(targetUserId)) {
            log.warn("[REMOVE_MEMBER_REJECTED] Admin cannot remove themselves. groupId={} adminId={}",
                    groupId, adminId);
            throw new IllegalStateException("Admin cannot remove themselves — use leaveGroup instead");
        }

        // 5 — Cannot remove another admin
        if (groupAdminService.isAdmin(groupId, targetUserId)) {
            log.warn("[REMOVE_MEMBER_REJECTED] Cannot remove another admin. groupId={} targetUserId={}",
                    groupId, targetUserId);
            throw new IllegalStateException("Cannot remove another admin from the group");
        }

        // 6 — Validate target user has active lifecycle
        participantLifecycleService.validateActiveParticipant(groupId, targetUserId);

        // 7 — End participant lifecycle
        participantLifecycleService.endParticipantLifecycle(groupId, targetUserId, LocalDateTime.now());


        log.info("[REMOVE_MEMBER_SUCCESS] groupId={} targetUserId={}", groupId, targetUserId);
    }

    @Transactional
    public void leaveGroup(Long groupId,Long userId){
        log.info("[LEAVE_MEMBER] groupId={} userId={} ",
                groupId, userId);

        // 1 — Validate conversation exists
        conversationService.getById(groupId);

        // 2 — Validate group is operational
        conversationLifecycleService.validateActive(groupId);

        // 3 — Validate adminId is an admin
        participantLifecycleService.validateActiveParticipant(groupId,userId);

        if (groupAdminService.isAdmin(groupId, userId)) {
            log.warn("[REMOVE_MEMBER_REJECTED] Admin cannot leave. groupId={} adminId={}",
                    groupId, userId);
            throw new IllegalStateException("Admin cannot leave from here — use removeAdmin instead");
        }

        participantLifecycleService.endParticipantLifecycle(groupId, userId,LocalDateTime.now());


        log.info("[LEAVE_MEMBER_SUCCESS] groupId={} userId={}", groupId, userId);
    }

    @Transactional
    public void deleteGroupFromChatList(Long groupId,Long userId){
        log.info("[DELETE_GROUP] groupId={} userId={} ",
                groupId, userId);
        conversationService.getById(groupId);

        participantService.validateParticipant(groupId,userId);

        if(participantLifecycleService.isParticipationActive(groupId,userId)){
            log.warn("[DELETE_GROUP] group still active groupId={} userId={}",
                    groupId, userId);
            throw new IllegalStateException("Group still active");
        }

        participantService.toggleVisible(groupId,userId);

        log.info("[DELETE_GROUP_SUCCESS] groupId={} userId={}", groupId, userId);



    }


}
