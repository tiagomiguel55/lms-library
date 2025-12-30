package pt.psoft.g1.psoftg1.shared.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.psoft.g1.psoftg1.shared.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find unprocessed events ordered by creation time
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents();

    /**
     * Find events that failed but can be retried
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.retryCount > 0 ORDER BY e.lastRetryAt ASC")
    List<OutboxEvent> findFailedEventsForRetry();

    /**
     * Delete old processed events (for cleanup)
     */
    void deleteByProcessedTrueAndProcessedAtBefore(LocalDateTime before);
}

