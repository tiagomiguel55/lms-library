package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "A Reader and User Creation Requested Event for AMQP communication")
@NoArgsConstructor
@AllArgsConstructor
public class ReaderUserRequestedEvent {

    @NotNull
    private String readerNumber;

    @NotNull
    private String username;

    @NotNull
    private String password;

    @NotNull
    private String fullName;

    @NotNull
    private String birthDate;

    @NotNull
    private String phoneNumber;

    private String photoURI;

    private boolean gdpr;

    private boolean marketing;

    private boolean thirdParty;
}
