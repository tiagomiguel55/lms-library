package pt.psoft.g1.psoftg1.bookmanagement.model.relational;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;

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


    @Embedded
    DescriptionEntity description;

    private void setTitle(String title) {this.title = new TitleEntity(title);}

    private void setIsbn(String isbn) {
        this.isbn = new IsbnEntity(isbn);
    }

    private void setDescription(String description) {this.description = new DescriptionEntity(description); }



    public String getDescription(){ return this.description.toString(); }

    public BookEntity(String isbn, String title, String description, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if(description != null)
            setDescription(description);

        setPhotoInternal(photoURI);
    }

    protected BookEntity() {
        // got ORM only
    }

    public String getIsbn(){
        return this.isbn.toString();
    }
}
