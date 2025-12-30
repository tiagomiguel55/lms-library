package pt.psoft.g1.psoftg1.bookmanagement.model.relational;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import pt.psoft.g1.psoftg1.genremanagement.model.relational.GenreEntity;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Book", uniqueConstraints = {
        @UniqueConstraint(name = "uc_book_isbn", columnNames = {"ISBN"})
})
public class BookEntity extends EntityWithPhoto {
    @Getter
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    long pk;

    @Version
    @Getter
    private Long version;

    @Embedded
    IsbnEntity isbn;

    @Getter
    @Embedded
    @NotNull
    TitleEntity title;

    @Getter
    @ManyToOne
    @NotNull
    GenreEntity genre;


    @Embedded
    DescriptionEntity description;

    private void setTitle(String title) {this.title = new TitleEntity(title);}

    private void setIsbn(String isbn) {
        this.isbn = new IsbnEntity(isbn);
    }

    private void setDescription(String description) {this.description = new DescriptionEntity(description); }

    public void setGenre(GenreEntity genre) {this.genre = genre; }


    public String getDescription(){ return this.description.toString(); }

    public BookEntity(String isbn, String title, String description, GenreEntity genre, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if(description != null)
            setDescription(description);
        if(genre==null)
            throw new IllegalArgumentException("Genre cannot be null");
        setGenre(genre);

        setPhotoInternal(photoURI);
    }

    protected BookEntity() {
        // got ORM only
    }

    public String getIsbn(){
        return this.isbn.toString();
    }
}
