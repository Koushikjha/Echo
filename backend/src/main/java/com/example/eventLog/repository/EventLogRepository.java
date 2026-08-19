package com.example.eventLog.repository;

import com.example.eventLog.entity.EventLog;
import com.example.eventLog.enums.AggregateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog,Long> {
    @Query("""
        SELECT COALESCE(MAX(e.version),0)
        FROM EventLog e
        WHERE e.aggregateType = :aggregateType
          AND e.aggregateId = :aggregateId
    """)
    Long findLatestVersion(
            AggregateType aggregateType,
            Long aggregateId
    );
}
