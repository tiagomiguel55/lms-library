package pt.psoft.g1.psoftg1.bookmanagement.model;


import lombok.Getter;
import lombok.Setter;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;





public class Book extends EntityWithPhoto {
    @Setter
    @Getter
    private Long version;
    private Isbn isbn;

    @Getter
    private Title title;

    Description description;

    private void setTitle(String title) {this.title = new Title(title);}

    private void setIsbn(String isbn) {
        this.isbn = new Isbn(isbn);
    }

    private void setDescription(String description) {this.description = new Description(description); }

    public String getDescription(){ return this.description.toString(); }

    public Book(String isbn, String title, String description, String photoURI) {
        setTitle(title);
        setIsbn(isbn);
        if(description != null)
            setDescription(description);

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


    // Get genre

    public String getIsbn(){
        return this.isbn.toString();
    }
}
