package com.example.chat.group.repo;

import com.example.chat.group.entities.GroupInvite;
import com.example.chat.group.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupInviteRepository extends JpaRepository<GroupInvite,Long> {
    boolean existsByGroupIdAndInvitedUserIdAndStatus(Long groupId, Long memberId, InviteStatus inviteStatus);
}
