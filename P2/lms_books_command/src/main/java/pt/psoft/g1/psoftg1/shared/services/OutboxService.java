package pt.psoft.g1.psoftg1.shared.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.shared.model.OutboxEvent;
import pt.psoft.g1.psoftg1.shared.repositories.OutboxEventRepository;

import java.util.List;

/**
 * Service for managing outbox events.
 * Events are stored in the database and will be published asynchronously.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save an event to the outbox table.
     * This should be called within the same transaction as the business operation.
     */
    @Transactional
    public OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, Object eventPayload) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, payload);
            return outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }

    /**
     * Get all unprocessed events
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getUnprocessedEvents() {
        return outboxEventRepository.findUnprocessedEvents();
    }

    /**
     * Mark an event as successfully processed
     */
    @Transactional
    public void markAsProcessed(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markAsProcessed();
            outboxEventRepository.save(event);
        });
    }

    /**
     * Record a failed publishing attempt
     */
    @Transactional
    public void recordFailure(Long eventId, String errorMessage) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.incrementRetryCount();
            event.setErrorMessage(errorMessage);
            outboxEventRepository.save(event);
        });
    }

    /**
     * Get events that need to be retried
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> getFailedEventsForRetry() {
        return outboxEventRepository.findFailedEventsForRetry();
    }
}

