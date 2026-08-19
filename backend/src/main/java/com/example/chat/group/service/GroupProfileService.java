package com.example.chat.group.service;

import com.example.chat.conversation.entity.Conversation;
import com.example.chat.dto.GroupProfileDTO;
import com.example.chat.group.entities.GroupProfile;
import com.example.chat.group.enums.GroupType;
import com.example.chat.group.repo.GroupProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupProfileService {
    private final GroupProfileRepository groupProfileRepository;

    @Transactional
    public GroupProfile createGroupProfile(
            Conversation conversation,
            String name,
            String description,
            String avatar,
            GroupType groupType,
            Long creatorId
    ) {

        log.info("[GROUP_PROFILE_CREATE_START] conversationId={} name={} creatorId={}",
                conversation.getId(), name, creatorId);

        try {

            GroupProfile profile = GroupProfile.builder()
                    .conversation(conversation)
                    .name(name)
                    .description(description)
                    .avatar(avatar)
                    .groupType(groupType)
                    .createdBy(creatorId)
                    .build();

            GroupProfile saved = groupProfileRepository.save(profile);

            log.info("[GROUP_PROFILE_CREATE_SUCCESS] groupProfileId={}", saved.getId());

            return saved;

        } catch (Exception ex) {

            log.error("[GROUP_PROFILE_CREATE_FAILED] conversationId={} reason={}",
                    conversation.getId(), ex.getMessage(), ex);

            throw new RuntimeException("Failed to create group profile", ex);
        }
    }

    @Transactional(readOnly = true)
    public GroupProfileDTO getGroupProfileDTO(Long groupId) {

        log.info("[GROUP_PROFILE_FETCH] groupId={}", groupId);

        try {
            GroupProfile profile = groupProfileRepository
                    .findByConversationId(groupId)
                    .orElseThrow(() -> {
                        log.error("[GROUP_PROFILE_NOT_FOUND] groupId={}", groupId);
                        return new RuntimeException("Group profile not found");
                    });

            GroupProfileDTO dto=GroupProfileDTO.builder()
                    .conversationId(profile.getConversation().getId())
                    .name(profile.getName())
                    .description(profile.getDescription())
                    .avatar(profile.getAvatar())
                    .groupType(profile.getGroupType())
                    .createdBy(profile.getCreatedBy())
                    .createdAt(profile.getCreatedAt())
                    .build();


            log.info("[GROUP_PROFILE_FETCH_SUCCESS] groupId={}", groupId);
            return dto;

        } catch (Exception e) {
            log.error("[GROUP_PROFILE_FETCH_FAILED] groupId={}", groupId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public GroupProfile getByGroupId(Long groupId) {

        log.info("[GROUP_PROFILE_FETCH] groupId={}", groupId);

        try {
            GroupProfile profile = groupProfileRepository
                    .findByConversationId(groupId)
                    .orElseThrow(() -> {
                        log.error("[GROUP_PROFILE_NOT_FOUND] groupId={}", groupId);
                        return new RuntimeException("Group profile not found");
                    });

            log.info("[GROUP_PROFILE_FETCH_SUCCESS] groupId={}", groupId);
            return profile;

        } catch (Exception e) {
            log.error("[GROUP_PROFILE_FETCH_FAILED] groupId={}", groupId, e);
            throw e;
        }
    }

    @Transactional
    public void updateProfile(GroupProfile profile,
                              String name,
                              String description,
                              String avatar) {

        Long groupId = profile.getConversation().getId();

        log.info("[GROUP_PROFILE_UPDATE] groupId={}", groupId);

        try {
            profile.setName(name);
            profile.setDescription(description);
            profile.setAvatar(avatar);

            groupProfileRepository.save(profile);

            log.info("[GROUP_PROFILE_UPDATE_SUCCESS] groupId={}", groupId);

        } catch (Exception e) {
            log.error("[GROUP_PROFILE_UPDATE_FAILED] groupId={}", groupId, e);
            throw e;
        }
    }

    @Transactional
    public void updateGroupType(GroupProfile profile, GroupType groupType) {

        Long groupId = profile.getConversation().getId();

        log.info("[GROUP_TYPE_UPDATE] groupId={} newType={}", groupId, groupType);

        try {
            profile.setGroupType(groupType);

            groupProfileRepository.save(profile);

            log.info("[GROUP_TYPE_UPDATE_SUCCESS] groupId={} type={}", groupId, groupType);

        } catch (Exception e) {
            log.error("[GROUP_TYPE_UPDATE_FAILED] groupId={} type={}", groupId, groupType, e);
            throw e;
        }
    }
}
