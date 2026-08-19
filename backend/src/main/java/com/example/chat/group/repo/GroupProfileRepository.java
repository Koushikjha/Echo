package com.example.chat.group.repo;

import com.example.chat.group.entities.GroupProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupProfileRepository extends JpaRepository<GroupProfile,Long> {
    @Query("""
        SELECT gp FROM GroupProfile gp
        WHERE gp.conversation.id = :conversationId
    """)
    Optional<GroupProfile> findByConversationId(Long conversationId);
}
