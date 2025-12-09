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

    public enum RequestStatus {
        PENDING_AUTHOR_CREATION,
        AUTHOR_CREATED,
        BOOK_CREATED,
        FAILED
    }

    public PendingBookRequest(String bookId, String authorName, String genreName) {
        this.bookId = bookId;
        this.authorName = authorName;
        this.genreName = genreName;
        this.requestedAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING_AUTHOR_CREATION;
    }
}

