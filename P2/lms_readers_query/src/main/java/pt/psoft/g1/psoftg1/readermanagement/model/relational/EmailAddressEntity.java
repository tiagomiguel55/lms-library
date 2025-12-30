package pt.psoft.g1.psoftg1.readermanagement.model.relational;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;

@Embeddable
@AllArgsConstructor
public class EmailAddressEntity {
    @Email
    String address;

    protected EmailAddressEntity() {}
}
