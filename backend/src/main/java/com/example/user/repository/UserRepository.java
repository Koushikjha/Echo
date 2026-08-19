package com.example.user.repository;

import com.example.user.entity.User;
import com.example.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByHandleName(String username);

    Optional<User> findById(long id);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<User> findByStatus(UserStatus status);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    void updateStatus(Long id, UserStatus status);

    boolean existsById(Long id);

    @Query("""
        SELECT u.handleName
        FROM User u
        WHERE u.id = :userId
    """)
    Optional<String> findHandleNameById(Long userId);

    @Query("""
    SELECT u.id FROM User u
    WHERE u.id IN :userIds
""")
    List<Long> findAllExistingIds(List<Long> userIds);

    @Query("""
    SELECT u
    FROM User u
    WHERE u.id <> :currentUserId
""")
    List<User> findAllExceptCurrentUser(
            @Param("currentUserId") Long currentUserId
    );

    @Modifying
    @Query("""
           UPDATE User u
           SET u.lastSeen = :time
           WHERE u.id = :userId
           """)
    int updateLastSeen(@Param("userId") Long userId,
                       @Param("time") LocalDateTime time);
}
