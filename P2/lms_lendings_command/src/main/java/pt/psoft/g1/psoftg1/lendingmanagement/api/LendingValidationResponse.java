package pt.psoft.g1.psoftg1.lendingmanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LendingValidationResponse {
    private String requestId;  // Mesmo ID do request para correlação
    private String lendingNumber;
    private boolean bookExists;
    private String isbn;
    private String message;
}


