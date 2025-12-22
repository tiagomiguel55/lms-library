package pt.psoft.g1.psoftg1.bookmanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "A Book form AMQP communication")
@NoArgsConstructor
public class BookViewAMQP {
    @NotNull
    @Getter
    private String isbn;

    @NotNull
    @Getter
    private String title;

    @NotNull
    @Getter
    private String description;

    @NotNull
    @Getter
    private List<Long> authorIds;

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

    public BookViewAMQP(String isbn, String title, String description, List<Long> authorIds, String genre) {
        this.isbn = isbn;
        this.title = title;
        this.description = description;
        this.authorIds = authorIds;
        this.genre = genre;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Long> getAuthorIds() {
        return authorIds;
    }

    public String getGenre() {
        return genre;
    }

    public Long getVersion() {
        return version;
    }
}
