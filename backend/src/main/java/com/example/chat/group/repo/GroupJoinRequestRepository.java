package com.example.chat.group.repo;

import com.example.chat.group.entities.GroupJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest,Long> {
}
