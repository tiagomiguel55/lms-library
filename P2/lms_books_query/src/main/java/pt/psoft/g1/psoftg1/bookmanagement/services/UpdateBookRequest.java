package pt.psoft.g1.psoftg1.bookmanagement.services;

import jakarta.annotation.Nullable;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;

@Getter
@Data
@NoArgsConstructor
public class UpdateBookRequest {
    @Setter
    @Getter
    private String isbn;

    @Setter
    @Getter
    private String description;

    @Getter
    private String title;

    @Nullable
    @Setter
    @Getter
    private String photoURI;

    @Nullable
    @Getter
    @Setter
    private MultipartFile photo;

    @Setter
    @Getter
    private Genre genreObj;

    @Getter
    private String genre;

    @Getter
    private List<Long> authors;

    @Getter
    private List<Author> authorObjList;

    public UpdateBookRequest(String isbn, String title, String genre, @NonNull List<Long> authors, String description) {
        this.isbn = isbn;
        this.genre = genre;
        this.title = title;
        this.description = description;
        this.authors = authors;
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

    public String getPhotoURI() {
        return photoURI;
    }

    public String getGenre() {
        return genre;
    }

    public List<Long> getAuthors() {
        return authors;
    }
}
