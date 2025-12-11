package pt.psoft.g1.psoftg1.genremanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "A Genre for AMQP communication")
@NoArgsConstructor
public class GenreViewAMQP {
    @NotNull
    private String genre;

    @NotNull
    private Long version;

    @Setter
    @Getter
    private String bookId;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();

    public GenreViewAMQP(String genre) {
        this.genre = genre;
    }
}
