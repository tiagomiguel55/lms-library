package pt.psoft.g1.psoftg1.bookmanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "A Book Requested Event for AMQP communication")
@NoArgsConstructor
@AllArgsConstructor
public class BookRequestedEvent {

    @NotNull
    private String bookId;

    @NotNull
    private String authorName;

    @NotNull
    private String genreName;
}
