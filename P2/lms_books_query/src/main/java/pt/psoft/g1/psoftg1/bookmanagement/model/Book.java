package pt.psoft.g1.psoftg1.bookmanagement.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "books")
public class Book extends EntityWithPhoto {
    @Id
    @Getter
    private String id;

    @Version
    @Getter
    private Long version;

    @Indexed(unique = true)
    private Isbn isbn;

    @Getter
    @NotNull
    private Title title;

    @Getter
    @DBRef
    @NotNull
    private Genre genre;

    @Getter
    @DBRef
    private List<Author> authors = new ArrayList<>();

    private Description description;

    private void setTitle(String title) {
        this.title = new Title(title);
    }

    private void setIsbn(String isbn) {
        this.isbn = new Isbn(isbn);
    }

    private void setDescription(String description) {
        this.description = new Description(description);
    }

    private void setGenre(Genre genre) {
        this.genre = genre;
    }

    private void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public String getDescription() {
        return this.description.toString();
    }

    public Book(String isbn, String title, String description, Genre genre, List<Author> authors, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if (description != null)
            setDescription(description);
        if (genre == null)
            throw new IllegalArgumentException("Genre cannot be null");
        setGenre(genre);
        if (authors == null)
            throw new IllegalArgumentException("Author list is null");
        if (authors.isEmpty())
            throw new IllegalArgumentException("Author list is empty");

        setAuthors(authors);
        setPhotoInternal(photoURI);
    }

    protected Book() {
        // for ORM only
    }

    public void removePhoto(long desiredVersion) {
        if (desiredVersion != this.version) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        setPhotoInternal(null);
    }

    public void applyPatch(final Long desiredVersion,
                           final String title,
                           final String description,
                           final String photoURI,
                           final Genre genre,
                           final List<Author> authors ) {

        if (!Objects.equals(this.version, desiredVersion))
            throw new ConflictException("Object was already modified by another user");

        if (title != null) {
            setTitle(title);
        }

        if (description != null) {
            setDescription(description);
        }

        if (genre != null) {
            setGenre(genre);
        }

        if (authors != null) {
            setAuthors(authors);
        }

        if (photoURI != null)
            setPhotoInternal(photoURI);

    }

    public String getIsbn() {
        return this.isbn.toString();
    }
}
