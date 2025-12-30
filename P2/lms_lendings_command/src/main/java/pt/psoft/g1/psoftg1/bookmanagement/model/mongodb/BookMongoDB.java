package pt.psoft.g1.psoftg1.bookmanagement.model.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.model.mongodb.EntityWithPhotoMongoDB;


@Document(collection = "books")
@EnableMongoAuditing
public class BookMongoDB extends EntityWithPhotoMongoDB {

    @Getter
    @Setter
    @Id
    private String bookId;

    @Getter
    @Setter
    @Version
    @Field("version")
    private Long version;

    @Getter
    @Field("isbn")
    private IsbnMongoDB isbn;

    @Getter
    @Field("title")
    private TitleMongoDB title;



    @Field("description")
    DescriptionMongoDB description;

    private void setTitle(String title) {
        this.title = new TitleMongoDB(title);
    }

    private void setIsbn(String isbn) {
        this.isbn = new IsbnMongoDB(isbn);
    }

    private void setDescription(String description) {
        this.description = new DescriptionMongoDB(description);
    }



    public String getDescription() {
        return this.description.toString();
    }

    public BookMongoDB(String isbn, String title, String description, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if(description != null)
            setDescription(description);
        setPhotoInternal(photoURI);
    }

    protected BookMongoDB() {
        // got ORM only
    }

    public void removePhoto(long desiredVersion) {
        if(desiredVersion != this.version) {
            throw new ConflictException("Provided version does not match latest version of this object");
        }

        setPhotoInternal(null);
    }

    public String getIsbn(){
        return this.isbn.toString();
    }

}
