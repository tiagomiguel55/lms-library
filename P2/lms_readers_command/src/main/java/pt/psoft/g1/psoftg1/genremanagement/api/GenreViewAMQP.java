package pt.psoft.g1.psoftg1.genremanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "A Genre form AMQP communication")
public class GenreViewAMQP {

    @NotNull
    @Size(max = 100)
    private String genre;

    private String version;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();
}