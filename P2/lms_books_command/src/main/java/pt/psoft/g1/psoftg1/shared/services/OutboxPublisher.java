package pt.psoft.g1.psoftg1.shared.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.OutboxEvent;

import java.util.List;

/**
 * Background job that reads events from the outbox table and publishes them to RabbitMQ.
 * Runs periodically to ensure reliable event delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${outbox.max-retries:5}")
    private int maxRetries;

    /**
     * Process outbox events every 5 seconds
     */
    @Scheduled(fixedDelayString = "${outbox.polling-interval:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxService.getUnprocessedEvents();

        if (!events.isEmpty()) {
            log.info("Processing {} outbox events", events.size());
        }

        for (OutboxEvent event : events) {
            try {
                // Check if event exceeded max retries
                if (event.hasExceededMaxRetries(maxRetries)) {
                    log.error("Event {} exceeded max retries ({}). Skipping.", event.getId(), maxRetries);
                    continue;
                }

                // Publish to RabbitMQ - determine exchange based on aggregate type
                String exchangeName = determineExchange(event.getAggregateType());

                log.debug("Publishing event {} - Type: {} - Aggregate: {}",
                    event.getId(), event.getEventType(), event.getAggregateId());

                rabbitTemplate.convertAndSend(
                    exchangeName,
                    event.getEventType(),
                    event.getPayload()
                );

                // Mark as processed
                outboxService.markAsProcessed(event.getId());

                log.debug("Successfully published event {}", event.getId());

            } catch (Exception e) {
                log.error("Failed to publish event {} - Retry count: {}",
                    event.getId(), event.getRetryCount(), e);

                outboxService.recordFailure(event.getId(), e.getMessage());
            }
        }
    }

    /**
     * Retry failed events every minute
     */
    @Scheduled(fixedDelayString = "${outbox.retry-interval:60000}")
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxService.getFailedEventsForRetry();

        if (!failedEvents.isEmpty()) {
            log.info("Retrying {} failed events", failedEvents.size());
        }

        for (OutboxEvent event : failedEvents) {
            try {
                if (event.hasExceededMaxRetries(maxRetries)) {
                    log.error("Event {} exceeded max retries. Giving up.", event.getId());
                    continue;
                }

                log.info("Retrying event {} (attempt {}/{})",
                    event.getId(), event.getRetryCount() + 1, maxRetries);

                String exchangeName = determineExchange(event.getAggregateType());

                rabbitTemplate.convertAndSend(
                    exchangeName,
                    event.getEventType(),
                    event.getPayload()
                );

                outboxService.markAsProcessed(event.getId());

                log.info("Successfully published event {} on retry", event.getId());

            } catch (Exception e) {
                log.error("Retry failed for event {}", event.getId(), e);
                outboxService.recordFailure(event.getId(), e.getMessage());
            }
        }
    }

    /**
     * Determine which exchange to use based on aggregate type
     */
    private String determineExchange(String aggregateType) {
        return switch (aggregateType) {
            case "Author" -> "q.authors.events";
            case "Genre" -> "q.genres.events";
            default -> "q.books.events"; // Book and default
        };
    }
}
