package pt.psoft.g1.psoftg1.bookmanagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PendingBookRequest")
@NoArgsConstructor
@Getter
@Setter
public class PendingBookRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookId; // ISBN

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false)
    private String genreName;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @Version
    private Long version;

    public enum RequestStatus {
        PENDING_AUTHOR_CREATION,      // Initial state: waiting for author pending event
        PENDING_GENRE_CREATION,        // Author pending received, waiting for genre pending
        AUTHOR_CREATED,                // Only used if author comes before genre (edge case)
        BOTH_PENDING_CREATED,          // Both temporary entities created (finalized=false) - READY TO TRIGGER FINALIZATION
        AUTHOR_FINALIZED,              // Author finalized, waiting for genre finalization
        GENRE_FINALIZED,               // Genre finalized, waiting for author finalization
        BOTH_FINALIZED,                // Both entities finalized (finalized=true) - READY TO CREATE BOOK
        BOOK_CREATED,                  // Book successfully created with finalized entities
        FAILED                         // Saga compensation - book creation aborted
    }

    public PendingBookRequest(String bookId, String authorName, String genreName) {
        this.bookId = bookId;
        this.authorName = authorName;
        this.genreName = genreName;
        this.requestedAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING_AUTHOR_CREATION;
    }
}
