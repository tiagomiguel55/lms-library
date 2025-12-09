package pt.psoft.g1.psoftg1.authormanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "An Author Pending Created Event for AMQP communication")
@NoArgsConstructor
@AllArgsConstructor
public class AuthorPendingCreated {

    @NotNull
    private Long authorId;

    @NotNull
    private String bookId;

    @NotNull
    private String authorName;

    @NotNull
    private String genreName;
}
