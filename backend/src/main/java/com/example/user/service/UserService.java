// com/gigshield/user/service/UserService.java
package com.example.user.service;

import com.example.auth.dto.RegisterRequest;
import com.example.user.dto.UpdateProfileRequest;
import com.example.user.dto.UserDTO;
import com.example.user.dto.UserResponse;
import com.example.user.entity.User;
import com.example.user.enums.UserRole;
import com.example.user.mapper.UserMapper;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;


    @Override
    public UserDetails loadUserByUsername(String phone)
            throws UsernameNotFoundException {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + phone));
    }

//    /**
//     * Called after OTP is verified.
//     * If user exists → login flow, return existing user.
//     * If user doesn't exist → registration flow, create and return.
//     */
//    @Transactional
//    public User findOrCreate(String phone, RegisterRequest registrationData) {
//        return userRepository.findByPhone(phone)
//                .orElseGet(() -> {
//                    if (registrationData == null) {
//                        throw new IllegalArgumentException(
//                                "New user must provide registration details " +
//                                        "(fullName, username)");
//                    }
//                    validateRegistrationData(registrationData);
//                    User newUser = userMapper.toEntity(registrationData);
//                    log.info("New user registered: phone={}", phone);
//                    return userRepository.save(newUser);
//                });
//    }

    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + phone));
    }

//    public Optional<User> getByPhone(String phone){
//        return userRepository.findByPhone(phone);
//    }



    public UserResponse getProfile(String phone) {
        return userMapper.toResponse(findByPhone(phone));
    }

    @Transactional
    public UserResponse updateProfile(String phone,
                                      UpdateProfileRequest request) {
        User user = findByPhone(phone);
        user.setFullName(request.getFullName());
        user.setHandleName(request.getHandleName());
        return userMapper.toResponse(userRepository.save(user));
    }


    public long countAll() {
        return userRepository.count();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

//    private void validateRegistrationData(RegisterRequest req) {
//        if (req.getFullName() == null || req.getFullName().isBlank()) {
//            throw new IllegalArgumentException(
//                    "fullName is required for registration");
//        }
//        if (req.getUsername() == null || req.getUsername().isBlank()) {
//            throw new IllegalArgumentException(
//                    "username is required for registration");
//        }
//    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {

        log.info("[USER_FETCH] userId={}", userId);

        try {

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("[USER_NOT_FOUND] userId={}", userId);
                        return new IllegalStateException("User not found");
                    });

            log.info("[USER_FETCH_SUCCESS] userId={}", userId);
            return user;

        } catch (Exception ex) {
            log.error("[USER_FETCH_FAILED] userId={}", userId, ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public void validateExists(Long userId) {

        log.info("[VALIDATE_USER_EXISTS] userId={}", userId);

        try {

            boolean exists = userRepository.existsById(userId);

            if (!exists) {
                log.error("[USER_NOT_FOUND] userId={}", userId);
                throw new IllegalStateException("User does not exist");
            }

            log.info("[USER_EXISTS_VALIDATED] userId={}", userId);

        } catch (Exception ex) {
            log.error("[USER_VALIDATION_FAILED] userId={}", userId, ex);
            throw ex;
        }
    }

    @Transactional
    public String getHandleNameById(Long userId) {
        log.info("[HANDLENAME_FETCH] userId={}", userId);

        try {

            String handleName = userRepository.findHandleNameById(userId)
                    .orElseThrow(() -> {
                        log.error("[HANDLENAME_NOT_FOUND] userId={}", userId);
                        return new IllegalStateException("User not found");
                    });

            log.info("[USER_FETCH_SUCCESS] userId={}", userId);
            return handleName;

        } catch (Exception ex) {
            log.error("[USER_FETCH_FAILED] userId={}", userId, ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public void validateAllExist(List<Long> userIds) {

        log.info("[USER_VALIDATE_ALL_EXIST_START] userIds={}", userIds);

        try {

            List<Long> foundIds = userRepository.findAllExistingIds(userIds);

            if (foundIds.size() != userIds.size()) {

                Set<Long> missing = new HashSet<>(userIds);
                missing.removeAll(foundIds);

                log.error("[USER_VALIDATE_ALL_EXIST_FAILED] missingUserIds={}", missing);

                throw new RuntimeException("Users not found: " + missing);
            }

            log.info("[USER_VALIDATE_ALL_EXIST_SUCCESS]");

        } catch (Exception ex) {

            log.error("[USER_VALIDATE_ALL_EXIST_EXCEPTION] reason={}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to validate users", ex);
        }
    }

    @Transactional
    public User findOrCreateBare(String phone, boolean[] isNew) {
        return userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    isNew[0] = true;
                    User newUser = User.builder()
                            .phone(phone)
                            .role(UserRole.ROLE_USER)      // don't default to ADMIN
                            .profileComplete(false)
                            .build();
                    log.info("New bare user created: phone={}", phone);
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public void completeProfile(String phone, String fullName, String handleName) {
        User user = findByPhone(phone);
        user.setFullName(fullName);
        user.setHandleName(handleName);
        user.setProfileComplete(true);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
        log.info("Profile completed for phone={}", phone);
    }

    public List<UserDTO> getAllUsersExcept(Long currentUserId) {
        try {

            log.info("Fetching all users except userId={}", currentUserId);

            List<UserDTO> users = userRepository
                    .findAllExceptCurrentUser(currentUserId)
                    .stream()
                    .map(user -> new UserDTO(
                            user.getId(),
                            user.getHandleName(),
                            user.getFullName(),
                            user.getPhone()
                    ))
                    .toList();

            log.info("Successfully fetched {} users for userId={}",
                    users.size(),
                    currentUserId);

            return users;

        } catch (Exception e) {

            log.error("Failed to fetch users for userId={}",
                    currentUserId,
                    e);

            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    public Long resolveUserId(UserDetails userDetails) {
        try {
            log.info("Resolving user id for phone: {}", userDetails.getUsername());

            User user = userRepository
                    .findByPhone(userDetails.getUsername())
                    .orElseThrow(() -> {
                        log.warn("User not found for phone: {}", userDetails.getUsername());
                        return new RuntimeException("User not found");
                    });

            log.info("Successfully resolved user id {} for phone {}",
                    user.getId(),
                    userDetails.getUsername());

            return user.getId();

        } catch (Exception e) {
            log.error("Failed to resolve user id for phone: {}",
                    userDetails.getUsername(),
                    e);

            throw new RuntimeException("Failed to resolve authenticated user", e);
        }
    }

    @Transactional
    public void updateLastSeen(Long userId,LocalDateTime time) {
        userRepository.updateLastSeen(userId,time);
    }
}