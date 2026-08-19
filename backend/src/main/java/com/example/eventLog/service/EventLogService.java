package com.example.eventLog.service;

import com.example.eventLog.entity.EventLog;
import com.example.eventLog.enums.AggregateType;
import com.example.eventLog.enums.EventType;
import com.example.eventLog.repository.EventLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logEvent(
            AggregateType aggregateType,
            Long aggregateId,
            EventType eventType,
            Long actorId,
            Object payload
    ) {

        log.info("[EVENT_LOG_CREATE] aggregateType={} aggregateId={} eventType={}",
                aggregateType, aggregateId, eventType);

        try {

            Long version = eventLogRepository.findLatestVersion(
                    aggregateType,
                    aggregateId
            ) + 1;

            EventLog event = EventLog.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .actorId(actorId)
                    .version(version)
                    .idempotencyKey(java.util.UUID.randomUUID().toString())
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();

            eventLogRepository.save(event);

            log.info(
                    "[EVENT_LOG_CREATE_SUCCESS] aggregateId={} version={} eventType={}",
                    aggregateId,
                    version,
                    eventType
            );

        } catch (Exception e) {

            log.error(
                    "[EVENT_LOG_CREATE_FAILED] aggregateType={} aggregateId={} eventType={}",
                    aggregateType,
                    aggregateId,
                    eventType,
                    e
            );

            throw new RuntimeException("Failed to persist event", e);
        }
    }
}
