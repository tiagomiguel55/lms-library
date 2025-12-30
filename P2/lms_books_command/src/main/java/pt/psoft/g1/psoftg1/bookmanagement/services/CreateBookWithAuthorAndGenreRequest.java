package pt.psoft.g1.psoftg1.bookmanagement.services;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Request to create a book with author and genre")
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookWithAuthorAndGenreRequest {

    @NotNull
    private String title;

    @NotNull
    private String authorName;

    @NotNull
    private String genreName;
}
