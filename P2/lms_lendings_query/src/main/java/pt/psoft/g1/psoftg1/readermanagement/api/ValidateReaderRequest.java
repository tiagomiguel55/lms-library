package pt.psoft.g1.psoftg1.readermanagement.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidateReaderRequest {
    private String readerId;
    private String correlationId;

    public ValidateReaderRequest(String readerId, String correlationId) {
        this.readerId = readerId;
        this.correlationId = correlationId;
    }
}
