package pt.psoft.g1.psoftg1.lendingmanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LendingValidationRequest {
    private String requestId;  // ID único do request para correlação
    private String isbn;
    private String lendingNumber;
}