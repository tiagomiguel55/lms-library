package pt.psoft.g1.psoftg1.bookmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a finalized book event that is waiting for its genre to be created
 * This handles out-of-order event processing (BookFinalized arrives before GenreCreated)
 */
@Document(collection = "pending_book_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PendingBookEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String bookId;

    @Indexed
    private String genreName;

    private Long authorId;

    private String authorName;

    private String title;

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

