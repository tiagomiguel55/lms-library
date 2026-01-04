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
    @Getter
    private String genre;

    @NotNull
    @Getter
    @Setter
    private Long version;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();

    @Getter
    private String bookId; // Associated book ISBN when genre is finalized

    public GenreViewAMQP(String genre) {
        this.genre = genre;
    }

    public String getGenre() {
        return genre;
    }
}
