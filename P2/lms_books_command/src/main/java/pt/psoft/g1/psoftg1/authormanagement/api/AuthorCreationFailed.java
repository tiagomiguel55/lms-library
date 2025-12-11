package pt.psoft.g1.psoftg1.authormanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "An Author Creation Failed Event for AMQP communication")
@NoArgsConstructor
@AllArgsConstructor
public class AuthorCreationFailed {

    @NotNull
    private String bookId;

    @NotNull
    private String authorName;

    @NotNull
    private String genreName;

    @NotNull
    private String errorMessage;
}

