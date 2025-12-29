package pt.psoft.g1.psoftg1.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Outbox pattern implementation for reliable event publishing.
 * Events are stored in the database in the same transaction as business data,
 * then published to RabbitMQ asynchronously.
 */
@Entity
@Table(name = "outbox_events")
@NoArgsConstructor
@Getter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "Book", "Author", "Genre"

    @Column(nullable = false)
    private String aggregateId; // e.g., ISBN, AuthorId

    @Column(nullable = false)
    private String eventType; // e.g., "BOOK_CREATED", "BOOK_UPDATED"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON payload

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column
    private LocalDateTime processedAt;

    @Column
    private Integer retryCount = 0;

    @Column
    private LocalDateTime lastRetryAt;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
        this.processed = false;
        this.retryCount = 0;
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    public boolean hasExceededMaxRetries(int maxRetries) {
        return this.retryCount >= maxRetries;
    }
}
