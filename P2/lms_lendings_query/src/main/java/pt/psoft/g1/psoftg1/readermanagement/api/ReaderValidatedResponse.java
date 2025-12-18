package pt.psoft.g1.psoftg1.readermanagement.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReaderValidatedResponse {
    private String readerId;
    private boolean exists;
    private String correlationId;

    public ReaderValidatedResponse(String readerId, boolean exists, String correlationId) {
        this.readerId = readerId;
        this.exists = exists;
        this.correlationId = correlationId;
    }
}
