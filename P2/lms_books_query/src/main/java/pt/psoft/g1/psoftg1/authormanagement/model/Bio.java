package pt.psoft.g1.psoftg1.authormanagement.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.data.annotation.Transient;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

@Getter
public class Bio {
    @Transient
    private final int BIO_MAX_LENGTH = 4096;

    @NotNull
    @Size(min = 1, max = BIO_MAX_LENGTH)
    private String bio;

    public Bio(String bio) {
        setBio(bio);
    }

    protected Bio() {
    }

    public void setBio(String bio) {
        if (bio == null)
            throw new IllegalArgumentException("Bio cannot be null");
        if (bio.isBlank())
            throw new IllegalArgumentException("Bio cannot be blank");
        if (bio.length() > BIO_MAX_LENGTH)
            throw new IllegalArgumentException("Bio has a maximum of 4096 characters");
        this.bio = StringUtilsCustom.sanitizeHtml(bio);
    }

    @Override
    public String toString() {
        return bio;
    }
}
