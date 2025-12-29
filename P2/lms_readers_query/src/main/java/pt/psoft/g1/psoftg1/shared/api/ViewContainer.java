package pt.psoft.g1.psoftg1.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewAMQP;
import pt.psoft.g1.psoftg1.genremanagement.api.GenreViewAMQP;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewAMQP;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewAMQP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "A Container form AMQP communication")
public class ViewContainer {

    @NotNull
    List<BookViewAMQP> books;

    @NotNull
    List<GenreViewAMQP> genres;

    @NotNull
    List<LendingViewAMQP> lendings;

    @NotNull
    List<ReaderViewAMQP> readers;

    @Setter
    @Getter
    private Map<String, Object> _links = new HashMap<>();
}
