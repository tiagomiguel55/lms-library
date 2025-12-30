package pt.psoft.g1.psoftg1.bookmanagement.model;


import lombok.Getter;
import lombok.Setter;
import org.hibernate.StaleObjectStateException;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Book extends EntityWithPhoto {

    private Long pk;
    @Setter
    @Getter
    private Long version;
    private Isbn isbn;

    @Getter
    private Title title;

    private Genre genre;

    Description description;

    private void setTitle(String title) {this.title = new Title(title);}

    private void setIsbn(String isbn) {
        this.isbn = new Isbn(isbn);
    }

    private void setDescription(String description) {this.description = new Description(description); }

    private void setGenre(Genre genre) {this.genre = genre; }


    public String getDescription(){ return this.description.toString(); }

    public Book(String isbn, String title, String description, Genre genre, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if(description != null)
            setDescription(description);
        if(genre==null)
            throw new IllegalArgumentException("Genre cannot be null");
        setGenre(genre);
        setPhotoInternal(photoURI);
    }

    protected Book() {
        // got ORM only
    }

    public void removePhoto(long desiredVersion) {
        if(desiredVersion != this.version) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        setPhotoInternal(null);
    }

    public void applyPatch(final Long desiredVersion,
                           final String title,
                           final String description,
                           final String photoURI,
                           final Genre genre) {

        if (!Objects.equals(this.version, desiredVersion))
            throw new StaleObjectStateException("Object was already modified by another user", this.pk);

        if (title != null) {
            setTitle(title);
        }

        if (description != null) {
            setDescription(description);
        }

        if (genre != null) {
            setGenre(genre);
        }

        if (photoURI != null)
            setPhotoInternal(photoURI);

    }

    // Get genre
    public Genre getGenre(){
        return this.genre;
    }

    public String getIsbn(){
        return this.isbn.toString();
    }
}
