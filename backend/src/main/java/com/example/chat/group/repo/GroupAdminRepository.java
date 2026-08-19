package com.example.chat.group.repo;

import com.example.chat.group.entities.GroupAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupAdminRepository extends JpaRepository<GroupAdmin,Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long creatorId);

    Optional<GroupAdmin> findByGroupIdAndUserId(Long groupId, Long userId);
}
