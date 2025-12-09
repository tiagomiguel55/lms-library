package pt.psoft.g1.psoftg1.authormanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "An Author for AMQP communication")
@NoArgsConstructor
public class AuthorViewAMQP {
    @NotNull
    private Long authorNumber;

    @NotNull
    private String name;

    private String bio;

    private String photoURI;

    @NotNull
    private Long version;

    private String bookId; // Associated book ISBN when author is finalized

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();

    public AuthorViewAMQP(Long authorNumber, String name, String bio, String photoURI) {
        this.authorNumber = authorNumber;
        this.name = name;
        this.bio = bio;
        this.photoURI = photoURI;
    }
}
