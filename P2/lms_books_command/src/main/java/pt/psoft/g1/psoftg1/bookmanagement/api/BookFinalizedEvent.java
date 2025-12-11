package pt.psoft.g1.psoftg1.bookmanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookFinalizedEvent {
    private Long authorId;
    private String authorName;
    private String bookId;
    private String genreName;
}
