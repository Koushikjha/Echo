package com.example.chat.group.service;

import com.example.chat.group.entities.GroupAdmin;
import com.example.chat.group.repo.GroupAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupAdminService {
    private final GroupAdminRepository groupAdminRepository;

    @Transactional
    public void addCreatorAsAdmin(Long groupId, Long creatorId) {

        log.info("[GROUP_ADMIN_ADD_CREATOR_START] groupId={} creatorId={}",
                groupId, creatorId);

        try {

            boolean alreadyAdmin =
                    groupAdminRepository.existsByGroupIdAndUserId(groupId, creatorId);

            if (alreadyAdmin) {
                log.warn("[GROUP_ADMIN_ALREADY_EXISTS] groupId={} userId={}",
                        groupId, creatorId);
                return;
            }

            GroupAdmin admin = GroupAdmin.builder()
                    .groupId(groupId)
                    .userId(creatorId)
                    .assignedBy(null) // original creator
                    .build();

            groupAdminRepository.save(admin);

            log.info("[GROUP_ADMIN_ADD_CREATOR_SUCCESS] groupId={} creatorId={}",
                    groupId, creatorId);

        } catch (Exception ex) {

            log.error("[GROUP_ADMIN_ADD_CREATOR_FAILED] groupId={} creatorId={} reason={}",
                    groupId, creatorId, ex.getMessage(), ex);

            throw new RuntimeException("Failed to add creator as group admin", ex);
        }
    }

    @Transactional(readOnly = true)
    public void validateAdmin(Long groupId, Long adminId) {

        log.info("[GROUP_ADMIN_VALIDATE] groupId={} adminId={}", groupId, adminId);

        try {
            boolean isAdmin = groupAdminRepository
                    .existsByGroupIdAndUserId(groupId, adminId);

            if (!isAdmin) {
                log.error("[GROUP_ADMIN_INVALID] groupId={} adminId={}", groupId, adminId);
                throw new RuntimeException("User is not an admin of this group");
            }

            log.info("[GROUP_ADMIN_VALID] groupId={} adminId={}", groupId, adminId);

        } catch (Exception e) {
            log.error("[GROUP_ADMIN_VALIDATE_FAILED] groupId={} adminId={}", groupId, adminId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(Long groupId, Long targetUserId) {

        log.info("[GROUP_ADMIN_CHECK] groupId={} userId={}",
                groupId, targetUserId);

        try {

            boolean isAdmin = groupAdminRepository
                    .existsByGroupIdAndUserId(groupId, targetUserId);

            log.info("[GROUP_ADMIN_CHECK_SUCCESS] groupId={} userId={} isAdmin={}",
                    groupId, targetUserId, isAdmin);

            return isAdmin;

        } catch (Exception e) {

            log.error("[GROUP_ADMIN_CHECK_FAILED] groupId={} userId={}",
                    groupId, targetUserId, e);

            throw e;
        }
    }

    @Transactional
    public void removeAdmin(Long groupId, Long userId) {

        log.info("[GROUP_ADMIN_REMOVE] groupId={} userId={}",
                groupId, userId);

        try {

            GroupAdmin admin = groupAdminRepository
                    .findByGroupIdAndUserId(groupId, userId)
                    .orElseThrow(() -> {
                        log.error("[GROUP_ADMIN_NOT_FOUND] groupId={} userId={}",
                                groupId, userId);
                        return new RuntimeException("Admin not found");
                    });

            groupAdminRepository.delete(admin);

            log.info("[GROUP_ADMIN_REMOVE_SUCCESS] groupId={} userId={}",
                    groupId, userId);

        } catch (Exception e) {

            log.error("[GROUP_ADMIN_REMOVE_FAILED] groupId={} userId={}",
                    groupId, userId, e);

            throw e;
        }
    }
}
