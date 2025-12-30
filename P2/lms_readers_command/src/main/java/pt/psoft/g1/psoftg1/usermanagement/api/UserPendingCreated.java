package pt.psoft.g1.psoftg1.usermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "User Pending Created Event for AMQP communication")
@NoArgsConstructor
@AllArgsConstructor
public class UserPendingCreated {

    @NotNull
    private String readerNumber;

    @NotNull
    private String userId;

    @NotNull
    private String username;

    private boolean finalized = false;
}
