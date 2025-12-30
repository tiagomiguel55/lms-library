package pt.psoft.g1.psoftg1.genremanagement.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "genres")
public class Genre {
    @Transient
    private final int GENRE_MAX_LENGTH = 100;

    @Id
    @Getter
    private String id;

    @Size(min = 1, max = GENRE_MAX_LENGTH, message = "Genre name must be between 1 and 100 characters")
    @Indexed(unique = true)
    @Getter
    private String genre;

    protected Genre() {
    }

    public Genre(String genre) {
        setGenre(genre);
    }

    private void setGenre(String genre) {
        if (genre == null)
            throw new IllegalArgumentException("Genre cannot be null");
        if (genre.isBlank())
            throw new IllegalArgumentException("Genre cannot be blank");
        if (genre.length() > GENRE_MAX_LENGTH)
            throw new IllegalArgumentException("Genre has a maximum of 4096 characters");
        this.genre = genre;
    }

    public String toString() {
        return genre;
    }
}
