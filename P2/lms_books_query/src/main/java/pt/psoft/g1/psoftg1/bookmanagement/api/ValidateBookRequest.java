package pt.psoft.g1.psoftg1.bookmanagement.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidateBookRequest {
    private String bookId;
    private String correlationId;

    public ValidateBookRequest(String bookId, String correlationId) {
        this.bookId = bookId;
        this.correlationId = correlationId;
    }
}
