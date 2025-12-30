package pt.psoft.g1.psoftg1.lendingmanagement.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookValidatedResponse {
    private String bookId;
    private boolean exists;
    private String correlationId;

    public BookValidatedResponse(String bookId, boolean exists, String correlationId) {
        this.bookId = bookId;
        this.exists = exists;
        this.correlationId = correlationId;
    }
}
