package pt.psoft.g1.psoftg1.bookmanagement.services;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A DTO for creating a Book with new Author(s) and Genre in one process")
public class CreateBookWithAuthorAndGenreRequest {

    // Book fields
    @NotBlank
    private String isbn;

    @NotBlank
    private String title;

    @Setter
    private String description;

    @Nullable
    @Getter
    @Setter
    private MultipartFile bookPhoto;

    @Nullable
    @Getter
    @Setter
    private String bookPhotoURI;

    // Genre fields
    @NotBlank
    @Size(min = 1, max = 100)
    private String genreName;

    // Author fields
    @NotNull
    @Size(min = 1)
    private List<AuthorData> authors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Author information for book creation")
    public static class AuthorData {
        @Size(min = 1, max = 150)
        private String name;

        @Size(min = 1, max = 4096)
        private String bio;

        @Nullable
        private MultipartFile photo;

        @Nullable
        private String photoURI;
    }
}

