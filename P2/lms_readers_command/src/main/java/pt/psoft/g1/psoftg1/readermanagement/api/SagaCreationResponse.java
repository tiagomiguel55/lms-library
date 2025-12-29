package pt.psoft.g1.psoftg1.readermanagement.api;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Data
public class SagaCreationResponse {

    private String lendingNumber;

    private String status;

    private String error;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();
}
