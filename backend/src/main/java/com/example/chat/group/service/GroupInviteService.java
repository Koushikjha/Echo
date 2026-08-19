package com.example.chat.group.service;

import com.example.chat.group.entities.GroupInvite;
import com.example.chat.group.enums.InviteStatus;
import com.example.chat.group.enums.InviteType;
import com.example.chat.group.repo.GroupInviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupInviteService {
    private final GroupInviteRepository groupInviteRepository;

    @Transactional
    public void createDirectInvite(Long groupId, Long memberId, Long creatorId) {

        log.info("[GROUP_INVITE_DIRECT_CREATE_START] groupId={} memberId={} by={}",
                groupId, memberId, creatorId);

        try {

            boolean alreadyPending =
                    groupInviteRepository
                            .existsByGroupIdAndInvitedUserIdAndStatus(
                                    groupId,
                                    memberId,
                                    InviteStatus.PENDING
                            );

            if (alreadyPending) {
                log.warn("[GROUP_INVITE_ALREADY_PENDING] groupId={} memberId={}",
                        groupId, memberId);
                return;
            }

            GroupInvite invite = GroupInvite.builder()
                    .groupId(groupId)
                    .invitedUserId(memberId)
                    .invitedBy(creatorId)
                    .type(InviteType.DIRECT)
                    .status(InviteStatus.PENDING)
                    .inviteCode(null)
                    .expiresAt(null)
                    .maxUses(null)
                    .build();

            groupInviteRepository.save(invite);

            log.info("[GROUP_INVITE_DIRECT_CREATE_SUCCESS] groupId={} memberId={}",
                    groupId, memberId);

        } catch (Exception ex) {

            log.error("[GROUP_INVITE_DIRECT_CREATE_FAILED] groupId={} memberId={} reason={}",
                    groupId, memberId, ex.getMessage(), ex);

            throw new RuntimeException("Failed to create direct group invite", ex);
        }
    }
}
