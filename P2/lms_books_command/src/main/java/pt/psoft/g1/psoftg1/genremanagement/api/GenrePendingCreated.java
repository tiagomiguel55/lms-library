package pt.psoft.g1.psoftg1.genremanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "A Genre Pending Created Event for AMQP communication")
@NoArgsConstructor
@AllArgsConstructor
public class GenrePendingCreated {

    @NotNull
    private String genreName;

    @NotNull
    private String bookId;
}
