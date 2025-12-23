package pt.psoft.g1.psoftg1.bookmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a finalized book event that is waiting for its genre to be created
 * This handles out-of-order event processing (BookFinalized arrives before GenreCreated)
 */
@Entity
@Table(name = "PendingBookEvent")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PendingBookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bookId;

    @Column(nullable = false)
    private String genreName;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private String authorName;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    public PendingBookEvent(String bookId, String genreName, Long authorId, String authorName, String title, String description) {
        this.bookId = bookId;
        this.genreName = genreName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.title = title;
        this.description = description;
    }
}

