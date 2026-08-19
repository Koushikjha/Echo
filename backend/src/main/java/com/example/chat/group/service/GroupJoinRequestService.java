package com.example.chat.group.service;

import com.example.chat.group.repo.GroupJoinRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class GroupJoinRequestService {
    private final GroupJoinRequestRepository groupJoinRequestRepository;
}
