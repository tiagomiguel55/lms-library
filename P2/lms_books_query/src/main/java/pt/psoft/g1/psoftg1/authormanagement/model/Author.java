package pt.psoft.g1.psoftg1.authormanagement.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.model.Photo;

@Document(collection = "authors")
public class Author extends EntityWithPhoto {
    @Id
    @Getter
    private String id;

    @Getter
    private long authorNumber;

    @Version
    private Long version;

    @Getter
    private String name;

    private Name name;

    private Bio bio;

    public void setName(String name) {
        if (name == null)
            throw new IllegalArgumentException("Name cannot be null");
        if (name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank, nor only white spaces");
        this.name = name;
    }

    public void setBio(String bio) {
        if (bio == null)
            throw new IllegalArgumentException("Bio cannot be null");
        if (bio.isBlank())
            throw new IllegalArgumentException("Bio cannot be blank");
        if (bio.length() > 4096)
            throw new IllegalArgumentException("Bio has a maximum of 4096 characters");
        this.bio = bio;
    }

    public Long getVersion() {
        return version;
    }

    public Long getId() {
        return authorNumber;
    }

    public Author(String name, String bio, String photoURI) {
        setName(name);
        setBio(bio);
        setPhotoInternal(photoURI);
    }

    public Author(long authorNumber, String name, String bio, String photoURI) {
        this.authorNumber = authorNumber;
        setName(name);
        setBio(bio);
        setPhotoInternal(photoURI);
    }

    public Author() {
        // for ORM only
    }

    public void applyPatch(final long desiredVersion, final String name, final String bio, final String photoURI) {
        if (this.version != desiredVersion)
            throw new ConflictException("Object was already modified by another user");
        if (request.getName() != null)
            setName(request.getName());
        if (request.getBio() != null)
            setBio(request.getBio());
        if (request.getPhotoURI() != null)
            setPhotoInternal(request.getPhotoURI());
    }

    public void removePhoto(long desiredVersion) {
        if (desiredVersion != this.version) {
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

    public void setPhotoURI(String photoURI) {
        setPhotoInternal(photoURI);
    }
}
