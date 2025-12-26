package pt.psoft.g1.psoftg1.bookmanagement.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.model.Photo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "books")
public class Book {
    @Id
    @Getter
    private String id;

    @Version
    @Getter
    private Long version;

    @Indexed(unique = true)
    private String isbn;

    @Getter
    @NotNull
    private String title;

    @Getter
    @DBRef
    @NotNull
    private Genre genre;

    @Getter
    @DBRef
    private List<Author> authors = new ArrayList<>();

    private String description;

    @Getter
    @Setter
    private Photo photo;

    private void setTitle(String title) {
        if (title == null)
            throw new IllegalArgumentException("Title cannot be null");
        if (title.isBlank())
            throw new IllegalArgumentException("Title cannot be blank");
        if (title.length() > 128)
            throw new IllegalArgumentException("Title has a maximum of 128 characters");
        this.title = title.strip();
    }

    private void setIsbn(String isbn) {
        if (!isValidIsbn(isbn)) {
            throw new IllegalArgumentException("Invalid ISBN format or check digit.");
        }
        this.isbn = isbn;
    }

    private static boolean isValidIsbn(String isbn) {
        if (isbn == null)
            throw new IllegalArgumentException("Isbn cannot be null");
        return (isbn.length() == 10) ? isValidIsbn10(isbn) : isValidIsbn13(isbn);
    }

    private static boolean isValidIsbn10(String isbn) {
        if (!isbn.matches("\\d{9}[\\dX]")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn.charAt(i) - '0') * (10 - i);
        }
        char lastChar = isbn.charAt(9);
        int lastDigit = (lastChar == 'X') ? 10 : lastChar - '0';
        sum += lastDigit;
        return sum % 11 == 0;
    }

    private static boolean isValidIsbn13(String isbn) {
        if (isbn == null || !isbn.matches("\\d{13}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Integer.parseInt(isbn.substring(i, i + 1));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == Integer.parseInt(isbn.substring(12));
    }

    private void setDescription(String description) {
        if (description == null || description.isBlank()) {
            this.description = null;
        } else if (description.length() > 4096) {
            throw new IllegalArgumentException("Description has a maximum of 4096 characters");
        } else {
            this.description = description;
        }
    }

    private void setGenre(Genre genre) {
        this.genre = genre;
    }

    private void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public String getDescription() {
        return this.description;
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
        if (!Objects.equals(desiredVersion, this.version)) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        setPhotoInternal(null);
    }

    protected void setPhotoInternal(String photoURI) {
        if (photoURI == null) {
            this.photo = null;
        } else {
            this.photo = new Photo(photoURI);
        }
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
        return this.isbn;
    }

    public String getTitleString() {
        return title;
    }
}
